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

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * 
 * @author Fabian Dill, KNIME.com AG
 */
public class TextMetaGUIElement extends MetaGUIElement {
    
    private static final String FORM_TYPE = "text";
    
    public TextMetaGUIElement(final String label, final String value, 
            final boolean isReadOnly) {
        super(label, value, isReadOnly);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Control createGUIElement(final FormToolkit toolkit, 
            final Composite parent) { 

            Text text = toolkit.createText(parent, getValue().trim(),
                    SWT.BORDER | SWT.FILL);
            text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            text.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    fireModifiedEvent(e);
                }
            });
            text.setEnabled(!isReadOnly());
            setControl(text);
            return text;
    }
    
    private Text getTextControl() {
        return (Text)getControl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveTo(final TransformerHandler parentElement) 
        throws SAXException {
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(null, null, MetaGUIElement.FORM, "CDATA", 
                FORM_TYPE);
        atts.addAttribute(null, null, MetaGUIElement.READ_ONLY, "CDATA", 
                "" + isReadOnly());
        atts.addAttribute(null, null, MetaGUIElement.NAME, "CDATA", getLabel());
        parentElement.startElement(null, null, MetaGUIElement.ELEMENT, atts);
        char[] value = getTextControl().getText().trim().toCharArray();
        parentElement.characters(value, 0, value.length);
        parentElement.endElement(null, null, MetaGUIElement.ELEMENT);
    }

}
