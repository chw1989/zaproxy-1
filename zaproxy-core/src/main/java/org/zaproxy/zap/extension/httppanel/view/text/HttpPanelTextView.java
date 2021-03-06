/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
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
package org.zaproxy.zap.extension.httppanel.view.text;

import java.awt.BorderLayout;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.configuration.FileConfiguration;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.httppanel.Message;
import org.zaproxy.zap.extension.httppanel.view.AbstractStringHttpPanelViewModel;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelView;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelViewModel;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelViewModelEvent;
import org.zaproxy.zap.extension.httppanel.view.HttpPanelViewModelListener;
import org.zaproxy.zap.extension.search.SearchMatch;
import org.zaproxy.zap.extension.search.SearchableHttpPanelView;
import org.zaproxy.zap.view.messagecontainer.http.SingleHttpMessageContainer;
import org.zaproxy.zap.view.messagecontainer.http.DefaultSingleHttpMessageContainer;

public abstract class HttpPanelTextView implements HttpPanelView, HttpPanelViewModelListener, SearchableHttpPanelView {

	/**
	 * Default name used for {@code MessageContainer}.
	 * 
	 * @see org.zaproxy.zap.view.messagecontainer.MessageContainer
	 */
	public static final String DEFAULT_MESSAGE_CONTAINER_NAME = "HttpMessagePanel";

	public static final String NAME = "HttpPanelTextView";
	
	private static final String CAPTION_NAME = Constant.messages.getString("http.panel.view.text.name");
	
	private HttpPanelTextArea httpPanelTextArea;
	private JPanel mainPanel;
	
	private AbstractStringHttpPanelViewModel model;
	
	/**
	 * The name that will be used for {@code MessageContainer}.
	 */
	private final String messageContainerName;
	
	public HttpPanelTextView(AbstractStringHttpPanelViewModel model) {
		this(DEFAULT_MESSAGE_CONTAINER_NAME, model);
	}

	public HttpPanelTextView(String messageContainerName, AbstractStringHttpPanelViewModel model) {
		this.model = model;
		
		this.messageContainerName = messageContainerName;
		init();
		
		this.model.addHttpPanelViewModelListener(this);
	}
	
	private void init() {
		httpPanelTextArea = createHttpPanelTextArea();
		httpPanelTextArea.setEditable(false);
		httpPanelTextArea.setLineWrap(true);
		httpPanelTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				showPopupMenuIfTriggered(e);
			}
			@Override
			public void mouseReleased(java.awt.event.MouseEvent e) {
				showPopupMenuIfTriggered(e);
			}
			private void showPopupMenuIfTriggered(java.awt.event.MouseEvent e) {
				// right mouse button action
				if (e.isPopupTrigger()) { 
					if (!httpPanelTextArea.isFocusOwner()) {
						httpPanelTextArea.requestFocusInWindow();
					}

					if (httpPanelTextArea.getMessage() instanceof HttpMessage) {
						SingleHttpMessageContainer messageContainer = new DefaultSingleHttpMessageContainer(
								messageContainerName,
								httpPanelTextArea,
								(HttpMessage) httpPanelTextArea.getMessage());
						View.getSingleton().getPopupMenu().show(messageContainer, e.getX(), e.getY());
					} else {
						View.getSingleton().getPopupMenu().show(httpPanelTextArea, e.getX(), e.getY());
					}
				}
			}
		});

		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(new JScrollPane(httpPanelTextArea), BorderLayout.CENTER);
	}
	
	protected abstract HttpPanelTextArea createHttpPanelTextArea();
	
	@Override
	public void setSelected(boolean selected) {
		if (selected) {
			httpPanelTextArea.requestFocusInWindow();
		}
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public String getCaptionName() {
		return CAPTION_NAME;
	}
	
	@Override
	public String getTargetViewName() {
		return "";
	}
	
	@Override
	public int getPosition() {
		return Integer.MIN_VALUE;
	}

	@Override
	public boolean isEnabled(Message msg) {
		return true;
	}

	@Override
	public boolean hasChanged() {
		return true;
	}
	
	@Override
	public JComponent getPane() {
		return mainPanel;
	}

	@Override
	public boolean isEditable() {
		return httpPanelTextArea.isEditable();
	}

	@Override
	public void setEditable(boolean editable) {
		httpPanelTextArea.setEditable(editable);
		if (!editable) {
			httpPanelTextArea.discardAllEdits();
		}
	}
	
	@Override
	public HttpPanelViewModel getModel() {
		return model;
	}
	
	@Override
	public void dataChanged(HttpPanelViewModelEvent e) {
		httpPanelTextArea.setMessage(model.getMessage());
		
		httpPanelTextArea.setText(model.getData());
		httpPanelTextArea.setCaretPosition(0);
	}
	
	@Override
	public void save() {
		model.setData(httpPanelTextArea.getText());
	}
	
	@Override
	public void search(Pattern p, List<SearchMatch> matches) {
		httpPanelTextArea.search(p, matches);
	}

	@Override
	public void highlight(SearchMatch sm) {
		httpPanelTextArea.highlight(sm);
	}
	
	@Override
	public void setParentConfigurationKey(String configurationKey) {
	}
	
	@Override
	public void loadConfiguration(FileConfiguration fileConfiguration) {
	}
	
	@Override
	public void saveConfiguration(FileConfiguration fileConfiguration) {
	}
}
