/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2010 psiinon@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.zap;

import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.DefaultFileSystem;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.log4j.*;
import org.apache.log4j.varia.NullAppender;

import org.parosproxy.paros.CommandLine;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.network.SSLConnector;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.control.ControlOverrides;
import org.zaproxy.zap.extension.autoupdate.ExtensionAutoUpdate;
import org.zaproxy.zap.extension.dynssl.DynSSLParam;
import org.zaproxy.zap.extension.dynssl.ExtensionDynSSL;
import org.zaproxy.zap.model.SessionUtils;
import org.zaproxy.zap.utils.ClassLoaderUtil;
import org.zaproxy.zap.utils.LocaleUtils;
import org.zaproxy.zap.view.LicenseFrame;
import org.zaproxy.zap.view.LocaleDialog;
import org.zaproxy.zap.view.ProxyDialog;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Locale;


public class ZAP {

    private static Logger log = null;
    private CommandLine cmdLine = null;

    static {
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger());

	    // set SSLConnector as socketfactory in HttpClient.
	    ProtocolSocketFactory sslFactory = null;
	    try {
	        final Protocol protocol = Protocol.getProtocol("https");
	        sslFactory = protocol.getSocketFactory();
	    } catch (final Exception e) {
			// Print the exception - log not yet initialised
	    	e.printStackTrace();
	    }
	    if (sslFactory == null || !(sslFactory instanceof SSLConnector)) {
	        Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory) new SSLConnector(), 443));
	    }
    }

	public static void main(String[] args) throws Exception {
	    final ZAP zap = new ZAP();
	    zap.init(args);

		// Nasty hack to prevent warning messages when running from the command line
		NullAppender na = new NullAppender();
		Logger.getRootLogger().addAppender(na);
		Logger.getRootLogger().setLevel(Level.OFF);
		Logger.getLogger(ConfigurationUtils.class).addAppender(na);
		Logger.getLogger(DefaultFileSystem.class).addAppender(na);

	    try {
	        Constant.getInstance();
	    } catch (final Throwable e) {
	    	// log not initialised yet
	        System.out.println(e.getMessage());
	        //throw e;
	        System.exit(1);
	    }
        final String msg = Constant.PROGRAM_NAME + " " + Constant.PROGRAM_VERSION + " started.";
        
		if (! zap.cmdLine.isGUI() && ! zap.cmdLine.isDaemon()) {
			// Turn off log4j somewhere if not gui or daemon
			Logger.getRootLogger().removeAllAppenders();
			Logger.getRootLogger().addAppender(na);
			Logger.getRootLogger().setLevel(Level.OFF);
		} else {
			BasicConfigurator.configure();
		}

        if (zap.cmdLine.isGUI()) {
            setViewLocale(Constant.getLocale());
        }
        
        log = Logger.getLogger(ZAP.class);
	    log.info(msg);

	    try {
	        zap.run();
	    } catch (final Exception e) {
	        log.fatal(e.getMessage(), e);
	        //throw e;
	        System.exit(1);
	    }

	}

    private static void setViewLocale(Locale locale) {
        JComponent.setDefaultLocale(locale);
        JOptionPane.setDefaultLocale(locale);
    }

    /**
	 * Initialization without dependence on any data model nor view creation.
	 * @param args
	 */
	private void init(String[] args) {
		
	    try {
	        cmdLine = new CommandLine(args);
	    } catch (final Exception e) {
	        System.out.println(CommandLine.getHelpGeneral());
	        System.exit(1);
	    }

		try {
			// lang directory includes all of the language files
			final File langDir = new File (Constant.getZapInstall(), "lang");
			if (langDir.exists() && langDir.isDirectory()) {
				ClassLoaderUtil.addFile(langDir.getAbsolutePath());
			} else {
				System.out.println("Warning: failed to load language files from " + langDir.getAbsolutePath());
			}
			// Load all of the jars in the lib directory
			final File libDir = new File(Constant.getZapInstall(), "lib");
			if (libDir.exists() && libDir.isDirectory()) {
				final File[] files = libDir.listFiles();
				for (final File file : files) {
					if (file.getName().toLowerCase(Locale.ENGLISH).endsWith("jar")) {
						ClassLoaderUtil.addFile(file);
					}
				}
			} else {
				System.out.println("Warning: failed to load jar files from " + libDir.getAbsolutePath());
			}
		} catch (final Exception e) {
			System.out.println("Failed loading jars: " + e);
		}

	}

	private void run() throws Exception {
	    
		final boolean isGUI = cmdLine.isGUI();
		
	    boolean firstTime = false;
	    if (isGUI) {
		    try {
		    	// Get the systems Look and Feel
		    	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

		    	// Set Nimbus LaF if available and system is not OSX
		    	if (!Constant.isMacOsX()) {
			        for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
			            if ("Nimbus".equals(info.getName())) {
			                UIManager.setLookAndFeel(info.getClassName());
			                break;
			            }
			        }
		    	}
		    } catch (final UnsupportedLookAndFeelException e) {
		        // handle exception
		    } catch (final ClassNotFoundException e) {
		        // handle exception
		    } catch (final InstantiationException e) {
		        // handle exception
		    } catch (final IllegalAccessException e) {
		        // handle exception
		    }
		    
		    firstTime = showLicense();
	    }

	    try {
			Model.getSingleton().init(this.getOverrides());
	    } catch (final java.io.FileNotFoundException e) {
	    	if (isGUI) {
	    		JOptionPane.showMessageDialog(null,
	    				Constant.messages.getString("start.db.error"),
	    				Constant.messages.getString("start.title.error"),
	    				JOptionPane.ERROR_MESSAGE);
	    	}
    		System.out.println(Constant.messages.getString("start.db.error"));
    		System.out.println(e.getLocalizedMessage());

	    	throw e;
	    }
	    Model.getSingleton().getOptionsParam().setGUI(isGUI);

		if (isGUI) {

			View.setDisplayOption(Model.getSingleton().getOptionsParam().getViewParam().getDisplayOption());

		    // Prompt for language if not set
			String locale = Model.getSingleton().getOptionsParam().getViewParam().getConfigLocale();
		    if (locale == null || locale.length() == 0) {
	        	// Dont use a parent of the MainFrame - that will initialise it with English!
				final Locale userloc = determineUsersSystemLocale();
		    	if (userloc == null) {
		    		// Only show the dialog, when the user's language can't be guessed.
					setViewLocale(Constant.getSystemsLocale());
					final LocaleDialog dialog = new LocaleDialog(null, true);
					dialog.init(Model.getSingleton().getOptionsParam());
					dialog.setVisible(true);
				} else {
					Model.getSingleton().getOptionsParam().getViewParam().setLocale(userloc);
				}
				setViewLocale(createLocale(Model.getSingleton().getOptionsParam().getViewParam().getLocale().split("_")));
				Constant.setLocale(Model.getSingleton().getOptionsParam().getViewParam().getLocale());
				Model.getSingleton().getOptionsParam().getViewParam().getConfig().save();
		    }

		    // Prompt for proxy details if set
		    if (Model.getSingleton().getOptionsParam().getConnectionParam().isProxyChainPrompt()) {
				final ProxyDialog dialog = new ProxyDialog(View.getSingleton().getMainFrame(), true);
				dialog.init(Model.getSingleton().getOptionsParam());
				dialog.setVisible(true);
		    }

		    runGUI();
		    

		    if (firstTime) {
		    	// Disabled for now - we have too many popups occuring when you first start up
		    	// be nice to have a clean start up wizard...
		    	// ExtensionHelp.showHelp();
		    } else {
		    	// Dont auto check for updates the first time, no chance of any proxy having been set
			    final ExtensionAutoUpdate eau = (ExtensionAutoUpdate)
			    		Control.getSingleton().getExtensionLoader().getExtension("ExtensionAutoUpdate");
			    if (eau != null) {
			    	eau.alertIfNewVersions();
			    }
		    }
		    
		    // check root certificate
		    final ExtensionDynSSL extension = (ExtensionDynSSL) Control.getSingleton().getExtensionLoader().getExtension("ExtensionDynSSL");
		    if (extension != null) {
			    DynSSLParam dynsslparam = extension.getParams();
			    if (dynsslparam.getRootca() == null) {
			    	// Create a new root cert in a background thread
			    	new Thread(new Runnable() {
			            @Override
			            public void run() {
					    	try {
								extension.createNewRootCa();
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
			            }
			        }).start();
			    }
		    }
	    } else if (cmdLine.isDaemon()) {
	    	runDaemon();
	    } else {
	        runCommandLine();
	    }

	}

	/**
	 * Determines the {@link Locale} of the current user's system.
	 * It will match the {@link Constant#getSystemsLocale()} with the available
	 * locales from ZAPs translation files. It may return null, if the users
	 * system locale is not in the list of available translations of ZAP.
	 * @return
	 */
	private Locale determineUsersSystemLocale() {
		Locale userloc = null;
		final Locale systloc = Constant.getSystemsLocale();
		// first, try full match
		for (String ls : LocaleUtils.getAvailableLocales()){
			String[] langArray = ls.split("_");
		    if (langArray.length == 1) {
		    	if (systloc.getLanguage().equals(langArray[0])) {
		    		userloc = systloc;
		    		break;
		    	}
		    }
		    if (langArray.length == 2) {
		    	if (systloc.getLanguage().equals(langArray[0]) && systloc.getCountry().equals(langArray[1])) {
		    		userloc = systloc;
		    		break;
		    	}
		    }
		    if (langArray.length == 3) {
		    	if (systloc.getLanguage().equals(langArray[0]) && systloc.getCountry().equals(langArray[1]) &&  systloc.getVariant().equals(langArray[2])) {
		    		userloc = systloc;
		    		break;
		    	}
		    }
		}
		if (userloc == null) {
			// second, try partial language match
			for (String ls : LocaleUtils.getAvailableLocales()){
				String[] langArray = ls.split("_");
				if (systloc.getLanguage().equals(langArray[0])) {
					userloc = createLocale(langArray);
					break;
				}
			}
		}
		return userloc;
	}
	
	private static Locale createLocale(String[] localeFields) {
		if (localeFields == null || localeFields.length == 0) {
			return null;
		}
		Locale.Builder localeBuilder = new Locale.Builder();
		localeBuilder.setLanguage(localeFields[0]);

		if (localeFields.length >= 2) {
			localeBuilder.setRegion(localeFields[1]);
		}
		if (localeFields.length >= 3) {
			localeBuilder.setVariant(localeFields[2]);
		}
		return localeBuilder.build();
	}

	private ControlOverrides getOverrides() {
		ControlOverrides overrides = new ControlOverrides();
		overrides.setProxyPort(this.cmdLine.getPort());
		overrides.setProxyHost(this.cmdLine.getHost());
		overrides.setConfigs(this.cmdLine.getConfigs());
		return overrides;
	}

	private void runCommandLine() {
	    int rc = 0;
	    String help = "";

	    Control.initSingletonWithoutView(this.getOverrides());
	    final Control control = Control.getSingleton();

	    // no view initialization

	    try {
	        control.getExtensionLoader().hookCommandLineListener(cmdLine);
	        if (cmdLine.isEnabled(CommandLine.HELP) || cmdLine.isEnabled(CommandLine.HELP2)) {
	            help = cmdLine.getHelp();
	            System.out.println(help);
	        } else if (cmdLine.isReportVersion()) {
	            System.out.println(Constant.PROGRAM_VERSION);
            } else {
                if (handleCmdLineSessionOptionsSynchronously(control)) {
                    control.runCommandLine();

                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                    }
                } else {
                    rc = 1;
                }
	        }
	    } catch (final Exception e) {
	        log.error(e.getMessage(), e);
	        System.out.println(e.getMessage());
	        System.out.println();
	        // Help is kind of useful too ;)
            help = cmdLine.getHelp();
            System.out.println(help);
	        rc = 1;
	    } finally {
            control.shutdown(Model.getSingleton().getOptionsParam().getDatabaseParam().isCompactDatabase());
    	    log.info(Constant.PROGRAM_TITLE + " terminated.");
	    }
	    System.exit(rc);
	}



	private void runGUI() throws ClassNotFoundException, Exception {

	    Control.initSingletonWithView(this.getOverrides());
	    final Control control = Control.getSingleton();
	    final View view = View.getSingleton();
	    view.postInit();
	    view.getMainFrame().setVisible(true);

        boolean createNewSession = true;
        if (cmdLine.isEnabled(CommandLine.SESSION) && cmdLine.isEnabled(CommandLine.NEW_SESSION)) {
            View.getSingleton().showWarningDialog(
                    Constant.messages.getString(
                            "start.gui.cmdline.invalid.session.options",
                            CommandLine.SESSION,
                            CommandLine.NEW_SESSION));
        } else if (cmdLine.isEnabled(CommandLine.SESSION)) {
            Path sessionPath = SessionUtils.getSessionPath(cmdLine.getArgument(CommandLine.SESSION));
            if (!sessionPath.isAbsolute()) {
                View.getSingleton().showWarningDialog(
                        Constant.messages.getString("start.gui.cmdline.session.absolute.path.required"));
            } else {
                if (!Files.exists(sessionPath)) {
                    View.getSingleton().showWarningDialog(
                            Constant.messages.getString("start.gui.cmdline.session.does.not.exist"));
                } else {
                    createNewSession = !control.getMenuFileControl().openSession(sessionPath.toAbsolutePath().toString());
                }
            }
        } else if (cmdLine.isEnabled(CommandLine.NEW_SESSION)) {
            Path sessionPath = SessionUtils.getSessionPath(cmdLine.getArgument(CommandLine.NEW_SESSION));
            if (!sessionPath.isAbsolute()) {
                View.getSingleton().showWarningDialog(
                        Constant.messages.getString("start.gui.cmdline.session.absolute.path.required"));
            } else {
                if (Files.exists(sessionPath)) {
                    View.getSingleton().showWarningDialog(
                            Constant.messages.getString("start.gui.cmdline.newsession.already.exist"));
                } else {
                    createNewSession = !control.getMenuFileControl().newSession(sessionPath.toAbsolutePath().toString());
                }
            }
        }

        if (createNewSession) {
            control.getMenuFileControl().newSession(false);
        }

        try {
        	// Allow extensions to pick up command line args in GUI mode
			control.getExtensionLoader().hookCommandLineListener(cmdLine);
			control.runCommandLine();
		} catch (Exception e) {
	        View.getSingleton().showWarningDialog(e.getMessage());
	        log.error(e.getMessage(), e);
		}
	}

	private void runDaemon() throws Exception {
		// start in a background thread
        final Thread t = new Thread(new Runnable() {
            @Override
			public void run() {
            	View.setDaemon(true);	// Prevents the View ever being initialised
        		Control.initSingletonWithoutView(getOverrides());
                Control control = Control.getSingleton();
                
                if (!handleCmdLineSessionOptionsSynchronously(control)) {
                    return;
                }
                
                try {
                    // Allow extensions to pick up command line args in daemon mode
                    control.getExtensionLoader().hookCommandLineListener(cmdLine);
                    control.runCommandLine();
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
        		// This is the only non-daemon thread, so should keep running
        		// CoreAPI.handleApiAction uses System.exit to shutdown
        		while (true) {
        			try {
						Thread.sleep(100000);
					} catch (InterruptedException e) {
						// Ignore
					}
        		}
            }});
        t.setName("ZAP-daemon");
        t.start();
	}

    private boolean handleCmdLineSessionOptionsSynchronously(Control control) {
        if (cmdLine.isEnabled(CommandLine.SESSION) && cmdLine.isEnabled(CommandLine.NEW_SESSION)) {
            System.err.println("Error: Invalid command line options: option '" + CommandLine.SESSION + "' not allowed with option '"
                    + CommandLine.NEW_SESSION + "'");
            return false;
        }

        if (cmdLine.isEnabled(CommandLine.SESSION)) {
            Path sessionPath = SessionUtils.getSessionPath(cmdLine.getArgument(CommandLine.SESSION));
            if (!sessionPath.isAbsolute()) {
                System.err.println("Error: Invalid command line value: option '" + CommandLine.SESSION
                        + "' requires an absolute path");
                return false;
            }
            String absolutePath = sessionPath.toAbsolutePath().toString();
            try {
                control.runCommandLineOpenSession(absolutePath);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                System.err.println("Failed to open session: " + absolutePath);
                e.printStackTrace(System.err);
                return false;
            }
        } else if (cmdLine.isEnabled(CommandLine.NEW_SESSION)) {
            Path sessionPath = SessionUtils.getSessionPath(cmdLine.getArgument(CommandLine.NEW_SESSION));
            if (!sessionPath.isAbsolute()) {
                System.err.println("Error: Invalid command line value: option '" + CommandLine.NEW_SESSION
                        + "' requires an absolute path");
                return false;
            }
            String absolutePath = sessionPath.toAbsolutePath().toString();
            if (Files.exists(sessionPath)) {
                System.err.println("Failed to create a new session, file already exists: " + absolutePath);
                return false;
            }

            try {
                control.runCommandLineNewSession(absolutePath);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                System.err.println("Failed to create a new session: " + absolutePath);
                e.printStackTrace(System.err);
                return false;
            }
        }

        return true;
    }

	private boolean showLicense() {
		boolean shown = false;
		
		File acceptedLicenseFile = new File(Constant.getInstance().ACCEPTED_LICENSE);
		
        if (!acceptedLicenseFile.exists()){
	        final LicenseFrame license = new LicenseFrame();
	        license.setVisible(true);
	        while (!license.isAccepted()) {
	            try {
	                Thread.sleep(100);
	            } catch (final InterruptedException e) {
	            	log.error(e.getMessage(), e);
	            }
	        }
	        shown = true;

	        try{
	            acceptedLicenseFile.createNewFile();
	        }catch (final IOException ie){
	            JOptionPane.showMessageDialog(new JFrame(), Constant.messages.getString("start.unknown.error"));
	            log.error(ie.getMessage(), ie);
	            System.exit(1);
	        }
	    }
	    
	    return shown;
	}

	private static final class UncaughtExceptionLogger implements Thread.UncaughtExceptionHandler {
		private static final Logger logger = Logger.getLogger(UncaughtExceptionLogger.class);

		private static boolean loggerConfigured = false;
		
		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if (!(e instanceof ThreadDeath)) {
				if (loggerConfigured || isLoggerConfigured()) {
					logger.error("Exception in thread \"" + t.getName() + "\"", e);
				} else {
					System.err.println("Exception in thread \"" + t.getName() + "\"");
					e.printStackTrace();
				}
			}
		}
		
		private static boolean isLoggerConfigured() {
			if (loggerConfigured) {
				return true;
			}
			
			@SuppressWarnings("unchecked")
			Enumeration<Appender> appenders = LogManager.getRootLogger().getAllAppenders();
			if (appenders.hasMoreElements()) {
				loggerConfigured = true;
			} else {
				
				@SuppressWarnings("unchecked")
				Enumeration<Logger> loggers = LogManager.getCurrentLoggers();
				while (loggers.hasMoreElements()) {
					Logger c = loggers.nextElement();
					if (c.getAllAppenders().hasMoreElements()) {
						loggerConfigured = true;
						break;
					}
				}
			}
			
			return loggerConfigured;
		}
	}

}
