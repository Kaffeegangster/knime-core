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
package org.knime.workbench.ui.metainfo.editor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.ui.metainfo.model.MetaGUIElement;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author Fabian Dill, KNIME.com AG
 */
public class WorkflowSetMetaInfoEditor extends EditorPart {


    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowSetMetaInfoEditor.class);

    private FormToolkit m_toolkit;
    private ScrolledForm m_form;

    private List<MetaGUIElement>m_elements = new ArrayList<MetaGUIElement>();

    private boolean m_isDirty = false;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        m_toolkit = new FormToolkit(parent.getDisplay());
        m_form = m_toolkit.createScrolledForm(parent);
        m_form.setText(getPartName());
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.makeColumnsEqualWidth = true;
        m_form.getBody().setLayout(layout);

        // content composite
        Composite content = m_toolkit.createComposite(m_form.getBody());
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.horizontalSpacing = 20;
        layout.makeColumnsEqualWidth = false;
        content.setLayout(layout);
        // placeholder composite
        m_toolkit.createComposite(m_form.getBody());

        GridData layoutData = new GridData();
        layoutData.minimumWidth = 100;
        layoutData.widthHint = 100;
        layoutData.verticalAlignment = SWT.TOP;
        for (MetaGUIElement element : m_elements) {
            LOGGER.debug("element " + element.getLabel());
            Label  label = m_toolkit.createLabel(content,
                    element.getLabel() + ": ");
            label.setLayoutData(layoutData);
            element.createGUIElement(m_toolkit, content);
            element.addListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent e) {
                    setDirty(true);
                }
            });
        }
        m_form.reflow(true);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        try {
            URI path = ((IURIEditorInput)getEditorInput()).getURI();
            File inputFile =  new File(path);

            SAXTransformerFactory fac
                = (SAXTransformerFactory)TransformerFactory.newInstance();
            TransformerHandler handler = fac.newTransformerHandler();

            Transformer t = handler.getTransformer();
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            OutputStream out = new FileOutputStream(inputFile);
            handler.setResult(new StreamResult(out));

            handler.startDocument();
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute(null, null, "nrOfElements", "CDATA", ""
                    + m_elements.size());
            handler.startElement(null, null, "KNIMEMetaInfo", atts);

            monitor.beginTask("Saving meta information...", m_elements.size());
            for (MetaGUIElement element : m_elements) {
                element.saveTo(handler);
                monitor.worked(1);
            }

            handler.endElement(null, null, "KNIMEMetaInfo");
            handler.endDocument();
            out.close();
            setDirty(false);
        } catch (Exception e) {
            LOGGER.error("An error ocurred while saving "
                    + getEditorInput().toString(), e);
        } finally {
            monitor.done();
        }
    }

    /**
     *
     * @param isDirty true if the editor should be set to dirty
     */
    public void setDirty(final boolean isDirty) {
        m_isDirty = isDirty;
        firePropertyChange(PROP_DIRTY);
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void doSaveAs() {
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input)
            throws PartInitException {
        super.setSite(site);
        super.setInput(input);
        if (!(input instanceof IURIEditorInput)) {
            throw new PartInitException("Unexpected input for "
                    + getClass().getName() + ": "
                    + input.getName());
        }
        URI path = ((IURIEditorInput)input).getURI();
        File inputFile = null;
        try {
            inputFile = new File(path);
        } catch (IllegalArgumentException iae) {
            if (!path.getScheme().equalsIgnoreCase("file")) {
                throw new PartInitException("Only local meta info can be edited", iae);
            }
            throw new PartInitException("Unexpected input for "
                    + getClass().getName() + ": "
                    + path, iae);
        }
        setPartName(new File(inputFile.getParent()).getName()
                + " : Meta Information");
        m_elements = parseInput(inputFile);
        LOGGER.debug("input = " + input.toString());
    }

    private List<MetaGUIElement>parseInput(final File inputFile)
        throws PartInitException {
        MetaInfoInputHandler hdl = new MetaInfoInputHandler();
        try {
            SAXParserFactory saxFac = SAXParserFactory.newInstance();
            saxFac.setNamespaceAware(true);
            SAXParser parser = saxFac.newSAXParser();
            parser.parse(inputFile, hdl);
        } catch (Exception e) {
            String msg = "Error while parsing input file "
                + inputFile.getName();
            LOGGER.error(msg, e);
            throw new PartInitException(msg, e);
        }
        return hdl.getElements();
    }


    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        return m_isDirty;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        m_form.setFocus();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        m_toolkit.dispose();
        super.dispose();
    }

}
