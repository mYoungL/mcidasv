/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.IdvXmlUi;
import ucar.unidata.ui.ComponentHolder;
import ucar.unidata.ui.HtmlComponent;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.xml.XmlUtil;

/**
 * <p>McIDAS-V mostly extends this class to preempt the IDV. McIDAS-V needs to
 * control some HTML processing, ensure that {@link McIDASVComponentGroup}s and
 * {@link McIDASVComponentHolder}s are created, and handle some special 
 * problems that occur when attempting to load bundles that do not contain
 * component groups.</p>
 */
@SuppressWarnings("unchecked")
public class McIDASVXmlUi extends IdvXmlUi {

	/** Avoid unneeded getIdv() calls. */
	private IntegratedDataViewer idv;

	/**
	 * Keep around a reference to the window we were built for, useful for
	 * associated component groups with the appropriate window. 
	 */
	private IdvWindow window;

	public McIDASVXmlUi(IdvWindow window, List viewManagers,
			IntegratedDataViewer idv, Element root) {
		super(window, viewManagers, idv, root);
		this.idv = idv;
		this.window = window;
	}

	/** 
	 * Convert the &amp;gt; and &amp;lt; entities to &gt; and &lt;.
	 * 
	 * @param text The text you'd like to convert.
	 * 
	 * @return The converted text!
	 */
	private static String decodeHtml(String text) {
		String html = text.replace("&gt;", ">");
		html = html.replace("&lt;", "<");
		return html;
	}

	/**
	 * <p>Overridden so that any attempts to generate 
	 * {@link ucar.unidata.idv.ui.IdvComponentGroup}s or
	 * {@link ucar.unidata.idv.ui.IdvComponentHolder}s will return the 
	 * respective McIDAS-V equivalents.</p>
	 * 
	 * <p>It makes things like the draggable tabs possible.</p>
	 * 
	 * @param node The XML representation of the desired component group.
	 * 
	 * @return An honest-to-goodness McIDASVComponentGroup based upon the 
	 *         contents of <code>node</code>.
	 * 
	 * @see ucar.unidata.idv.ui.IdvXmlUi#makeComponentGroup(Element)
	 */
	@Override protected IdvComponentGroup makeComponentGroup(Element node) {
		McIDASVComponentGroup group = new McIDASVComponentGroup(idv, "", window);
		group.initWith(node);

		NodeList elements = XmlUtil.getElements(node);
		for (int i = 0; i < elements.getLength(); i++) {
			Element child = (Element)elements.item(i);

			String tag = child.getTagName();

			if (tag.equals(IdvUIManager.COMP_MAPVIEW)
					|| tag.equals(IdvUIManager.COMP_VIEW)) {
				ViewManager viewManager = getViewManager(child);
				group.addComponent(new McIDASVComponentHolder(idv, viewManager));
			}
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_CHOOSERS)) {
				IdvComponentHolder comp = new McIDASVComponentHolder(idv,"choosers");
				comp.setType(IdvComponentHolder.TYPE_CHOOSERS);
				comp.setName(XmlUtil.getAttribute(child,"name","Choosers"));
				group.addComponent(comp);
			}
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_SKIN)) {
				IdvComponentHolder comp = new McIDASVComponentHolder(idv, XmlUtil.getAttribute(child, "url"));
				comp.setType(IdvComponentHolder.TYPE_SKIN);
				comp.setName(XmlUtil.getAttribute(child, "name", "UI"));
				group.addComponent(comp);
			}
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_HTML)) {
				String text = XmlUtil.getChildText(child);
				text = new String(XmlUtil.decodeBase64(text.trim()));
				ComponentHolder comp = new HtmlComponent("Html Text", text);
				comp.setShowHeader(false);
				comp.setName(XmlUtil.getAttribute(child,"name","HTML"));
				group.addComponent(comp);
				
			}
			else if (tag.equals(IdvUIManager.COMP_DATASELECTOR)) {
				group.addComponent(new McIDASVComponentHolder(idv,
						idv.getIdvUIManager().createDataSelector(false,
								false)));
			} 
			else if (tag.equals(IdvUIManager.COMP_COMPONENT_GROUP)) {
				group.addComponent(makeComponentGroup(child));
			}
			else {
				System.err.println("Unknown component element:" + XmlUtil.toString(child));
			}
		}
		return group;
	}

	/**
	 * <p>McIDAS-V overrides this so that it can seize control of some HTML
	 * processing in addition to attempting to associate newly-created 
	 * {@link ucar.unidata.idv.ViewManager}s with ViewManagers found in a
	 * bundle.</p>
	 * 
	 * <p>The latter is done so that McIDAS-V can load bundles that do not use
	 * component groups. A &quot;dynamic skin&quot; is built with ViewManagers 
	 * for each ViewManager in the bundle. The &quot;viewid&quot; attribute of
	 * the dynamic skin ViewManager is the name of the 
	 * {@link ucar.unidata.idv.ViewDescriptor} from the bundled ViewManager.
	 * <tt>createViewManager()</tt> is used to actually associate the new
	 * ViewManager with its bundled ViewManager.</p>
	 * 
	 * @param node The XML describing the component to be created.
	 * @param id <tt>node</tt>'s ID.
	 * 
	 * @return The {@link java.awt.Component} described by <tt>node</tt>.
	 * 
	 * @see ucar.unidata.idv.ui.IdvXmlUi#createComponent(Element, String)
	 * @see edu.wisc.ssec.mcidasv.ui.McIDASVXmlUi#createViewManager(Element)
	 */
	@Override public Component createComponent(Element node, String id) {
		Component comp = null;
		String tagName = node.getTagName();
		if (tagName.equals(TAG_HTML)) {
			String text = getAttr(node, ATTR_TEXT, NULLSTRING);
			text = decodeHtml(text);
			if (text == null) {
				String url = getAttr(node, ATTR_URL, NULLSTRING);
				if (url != null) {
					text = IOUtil.readContents(url, (String) null);
				}
				if (text == null) {
					text = XmlUtil.getChildText(node);
				}
			}
			HyperlinkListener linkListener = new HyperlinkListener() {
				public void hyperlinkUpdate(HyperlinkEvent e) {
					String url;
					if (e.getURL() == null) {
						url = e.getDescription();
					} else {
						url = e.getURL().toString();
					}
					actionPerformed(new ActionEvent(this, 0, url));
				}
			};
			Component[] comps = GuiUtils.getHtmlComponent(text, linkListener,
									getAttr(node, ATTR_WIDTH, 200),
									getAttr(node, ATTR_HEIGHT, 200));
			comp = comps[1];
		} 
		else if (tagName.equals(UIManager.COMP_MAPVIEW) || 
				 tagName.equals(UIManager.COMP_VIEW)) {

			// if we're creating a VM for a dynamic skin that was created for
			// a bundle, createViewManager() will return the bundled VM.
			ViewManager vm = createViewManager(node);
			if (vm != null)
				comp = vm.getContents();
			else
				comp = super.createComponent(node, id);
		}
		else {
			comp = super.createComponent(node, id);
		}

		return comp;
	}

	/**
	 * <p>Attempts to build a {@link ucar.unidata.idv.ViewManager} based upon
	 * <tt>node</tt>. If the XML has a &quot;viewid&quot; attribute, the value
	 * will be used to search for a ViewManager that has been cached by the
	 * McIDAS-V {@link UIManager}. If the UIManager has a matching ViewManager,
	 * we'll use the cached ViewManager to initialize a &quot;blank&quot;
	 * ViewManager. The cached ViewManager is then removed from the cache and
	 * deleted. If there wasn't a cached ViewManager, the blank ViewManager is
	 * returned.</p>
	 * 
	 * <p>In practice, ViewManagers rarely have a &quot;viewid&quot; attribute.
	 * The only ViewManagers that should have it will have been created by a
	 * dynamic skin so McIDAS-V can load bundles that do not contain component 
	 * groups. Additionally, the only &quot;cached&quot; ViewManagers will be 
	 * those that have been unpersisted by the IDV. This simulates 
	 * &quot;injecting&quot; a ViewManager stored in a bundle into a dynamic 
	 * skin.</p>
	 * 
	 * @param node The XML description of the ViewManager that needs building.
	 * 
	 * @return Either a &quot;blank&quot; ViewManager or one that has been 
	 *         initialized by a bundled ViewManager.
	 * 
	 * TODO: add @see stuff once you stabilize the UIManager code!!
	 */
	private ViewManager createViewManager(final Element node) {
		final String viewId = getAttr(node, "viewid", NULLSTRING);
		ViewManager vm = null;

		if (viewId != null) {
			vm = getViewManager(node);

			final ViewManager old = UIManager.savedViewManagers.remove(viewId);

			if (old != null) {
				vm.initWith(old);
				old.destroy();
			}
		}

		return vm;
	}
}
