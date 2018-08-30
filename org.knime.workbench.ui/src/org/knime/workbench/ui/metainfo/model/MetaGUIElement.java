/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.ui.metainfo.model;

import java.util.HashSet;
import java.util.Set;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.knime.workbench.ui.metainfo.editor.WorkflowSetMetaInfoEditor;
import org.xml.sax.SAXException;

/**
 * 
 * @author Fabian Dill, KNIME.com AG
 */
public abstract class MetaGUIElement {

    /** XML element 'element'. */
    public static final String ELEMENT = "element";

    /** XML element 'form'. */
    public static final String FORM = "form";

    /** XML element 'text'. */
    public static final String TEXT = "text";

    /** XML element 'name'. */
    public static final String NAME = "name";

    /** XML element 'date'. */
    public static final String DATE = "date";

    /** XML element 'multiline'. */
    public static final String MULTILINE = "multiline";

    /** XML element 'read-only'. */
    public static final String READ_ONLY = "read-only";

    private final String m_label;

    private final String m_value;

    private final boolean m_isReadOnly;

    private Control m_control;

    /**
     * Modification listener, for example to set the editor dirty. Each deriving
     * class has to take care to register a {@link ModifyListener} to the
     * wrapped control and forward a modify event of the wrapped control via the
     * {@link #fireModifiedEvent(ModifyEvent)} method.
     * 
     * @see WorkflowSetMetaInfoEditor#createPartControl(Composite)
     */
    private final Set<ModifyListener> m_listener 
        = new HashSet<ModifyListener>();

    /**
     * 
     * @param label the label of the element
     * @param value the value inside the edit control
     * @param readOnly true if it is read only
     */
    public MetaGUIElement(final String label, final String value,
            final boolean readOnly) {
        m_label = label;
        m_value = value;
        m_isReadOnly = readOnly;
    }

    /**
     * 
     * @return the label of this element
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * 
     * @return the value of this element
     */
    protected String getValue() {
        return m_value;
    }

    /**
     * 
     * @return true if element is read only
     */
    protected boolean isReadOnly() {
        return m_isReadOnly;
    }

    /**
     * 
     * @param control the underlying control, which is created in the
     *            implementation of
     *            {@link #createGUIElement(FormToolkit, Composite)}
     */
    protected void setControl(final Control control) {
        m_control = control;
    }

    /**
     * 
     * @return the control which was created by the
     *         {@link #createGUIElement(FormToolkit, Composite)} and set with
     *         {@link #setControl(Control)}.
     */
    protected Control getControl() {
        return m_control;
    }

    /**
     * 
     * @param listener a listener to be informed via the
     *            {@link #fireModifiedEvent(ModifyEvent)} if the underlying
     *            {@link Control} was modified
     */
    public void addListener(final ModifyListener listener) {
        m_listener.add(listener);
    }

    /**
     * Call this method whenever the udnerlying wrapped {@link Control} was
     * modified, the event will be forwarded to all registered
     * {@link ModifyListener}.
     * 
     * @param e event to be forwarded from the wrapped {@link Control} to the
     *            registered {@link ModifyListener}
     */
    protected void fireModifiedEvent(final ModifyEvent e) {
        for (ModifyListener l : m_listener) {
            l.modifyText(e);
        }
    }

    /**
     * Creates and returns the referring GUI element. The element must also be
     * stored locally with {@link #setControl(Control)} in order to be able to
     * store the changes in the GUI element in the
     * {@link #saveTo(TransformerHandler)} method. The element can then be
     * accessed with {@link #getControl()}. Since this is a specialized
     * {@link Control} implementing classes have to register a
     * {@link ModifyListener} and forward the events via
     * {@link #fireModifiedEvent(ModifyEvent)}.
     * 
     * The following lines show a recommended implementation: <code>
        Control someControl = toolkit.createControl(parent, getValue().trim(), 
             style);
        someControl.addModifyListener(new ModifyListener() {
         
            public void modifyText(final ModifyEvent e) {
                fireModifiedEvent(e);
             }
        });
         setControl(someControl);
         return someControl;
     * </code>
     * 
     * @param toolkit parent element
     * @param parent the parent control
     * @return the created {@link Control}
     */
    public abstract Control createGUIElement(final FormToolkit toolkit,
            final Composite parent);

    /**
     * Save the new value entered in the GUI element to the XML element, which
     * has the following syntax: <code>
     * &lt;element form="text" name="Author"&gt;Dill&lt;/element&gt;
     * </code>
     * where <em>form</em> is the GUI element to be displayed, <em>name</em>
     * is the label.
     * 
     * @param parentElement to add the complete element to
     * @throws SAXException if something goes wrong
     */
    public abstract void saveTo(final TransformerHandler parentElement)
            throws SAXException;

    /**
     * Factory method to create a {@link MetaGUIElement} with the parsed type 
     * of element, value of the element and whether it is read-only.
     * 
     * @param form type of form: one of (text, date, multiline)
     * @param label label of the element
     * @param value value to be editable within the GUI element
     * @param isReadOnly true if the field should not be edited by the user
     * @return the create GUI element
     */
    public static MetaGUIElement create(final String form, final String label,
            final String value, final boolean isReadOnly) {
        MetaGUIElement element = null;
        if (form.trim().equals(TEXT)) {
            element = new TextMetaGUIElement(label, value, isReadOnly);
        } else if (form.trim().equals(DATE)) {
            element = new DateMetaGUIElement(label, value, isReadOnly);
        } else if (form.trim().equals(MULTILINE)) {
            element = new MultilineMetaGUIElement(label, value, isReadOnly);
        }
        return element;
    }
}
