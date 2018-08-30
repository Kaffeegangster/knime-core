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
 *
 * History
 *   Oct 4, 2011 (wiswedel): created
 */
package org.knime.core.data.uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ConvenienceMethods;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class URIPortObject extends AbstractSimplePortObject implements IURIPortObject {
    /**
     * @noreference This class is not intended to be referenced by clients.
     * @since 3.0
     */
    public static final class Serializer extends AbstractSimplePortObjectSerializer<URIPortObject> {} 

    private URIPortObjectSpec m_uriPortObjectSpec;
    private List<URIContent> m_uriContents;

    /**
     * Framework constructor. <b>Do not use in client code.</b>
     */
    public URIPortObject() {
        // filled later in load
    }

    /**
     * Constructor for new URI port objects.
     * @param uriContents The contend for this object. Must not be null or contain null.
     */
    public URIPortObject(final List<URIContent> uriContents) {
        this(URIPortObjectSpec.create(uriContents), uriContents);
    }
    
    /**
     * Constructor for new URI port objects.
     * @param spec The non null spec. All file extensions in the spec must be present in the list argument.
     * @param uriContents The contend for this object. Must not be null or contain null.
     */
    public URIPortObject(URIPortObjectSpec spec, final List<URIContent> uriContents) {
        Set<String> extensionsSet = new HashSet<String>(spec.getFileExtensions());
        for (URIContent u : uriContents) {
            extensionsSet.remove(u.getExtension());
        }
        if (!extensionsSet.isEmpty()) {
            throw new IllegalArgumentException("Spec contains file extensions that are not present in the URI list: "
                    + ConvenienceMethods.getShortStringFrom(extensionsSet, 3));
        }
        m_uriContents = Collections.unmodifiableList(new ArrayList<URIContent>(uriContents));
        m_uriPortObjectSpec = spec;
    }

    /**
     * @return The uriContent of this object.
     */
    @Override
    public List<URIContent> getURIContents() {
        return m_uriContents;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSummary() {
        StringBuilder b = new StringBuilder();
        int size = m_uriContents.size();
        b.append(size);
        b.append(size == 1 ? " file (extension: " : " files (extensions: ");
        b.append(ConvenienceMethods.getShortStringFrom(m_uriPortObjectSpec.getFileExtensions(), 3));
        b.append(")");
        return b.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URIPortObjectSpec getSpec() {
        return m_uriPortObjectSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void load(final ModelContentRO model, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws InvalidSettingsException,
            CanceledExecutionException {
        List<URIContent> list = new ArrayList<URIContent>();
        for (String key : model.keySet()) {
            if (key.startsWith("child-")) {
                ModelContentRO child = model.getModelContent(key);
                list.add(URIContent.load(child));
            }
        }
        m_uriContents = Collections.unmodifiableList(list);
        m_uriPortObjectSpec = (URIPortObjectSpec)spec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void save(final ModelContentWO model, final ExecutionMonitor exec)
            throws CanceledExecutionException {
        int i = 0;
        for (URIContent uri : m_uriContents) {
            ModelContentWO child = model.addModelContent("child-" + i);
            uri.save(child);
            i++;
        }
    }

}
