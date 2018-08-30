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
 * -------------------------------------------------------------------
 *
 * History
 *   Mar 20, 2012 (morent): created
 */

package org.knime.workbench.repository.model;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSetFactory;
import org.osgi.framework.FrameworkUtil;

/**
 * A node template for dynamic nodes. Additional to the node factory class, a
 * {@link NodeSetFactory} class and the node factory ID is needed, to restore
 * the underlying node.
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 *
 */
public class DynamicNodeTemplate extends NodeTemplate {

    /**
     * Separator to separate the node factory's class name from the node's in order to construct the id:
     * <code>&#60;node-factory class name&#62;#&#60;node name&#62;</code>.
     */
    private static final String NODE_NAME_SEP = "#";

    private NodeSetFactory m_nodeSetFactory;

    private final String m_factoryId;

    /**
     * Constructs a new DynamicNodeTemplate.
     *
     * @param factoryClass the factory class
     * @param factoryId The id of the NodeFactory, must not be <code>null</code>
     * @param nodeSetFactory the NodeSetFactory that created this DynamicNodeTemplate, must not be <code>null</code>
     * @param name the name of this repository entry, must not be <code>null</code>
     */
    public DynamicNodeTemplate(final Class<NodeFactory<? extends NodeModel>> factoryClass, final String factoryId,
        final NodeSetFactory nodeSetFactory, final String name) {
        super(factoryClass.getName() + NODE_NAME_SEP + name, name,
            FrameworkUtil.getBundle(nodeSetFactory.getClass()).getSymbolicName());
        setFactory(factoryClass);
        m_factoryId = factoryId;
        m_nodeSetFactory = nodeSetFactory;
    }

    /**
     * Creates a copy of the given object.
     *
     * @param copy the object to copy
     */
    protected DynamicNodeTemplate(final DynamicNodeTemplate copy) {
        super(copy);
        this.m_nodeSetFactory = copy.m_nodeSetFactory;
        this.m_factoryId = copy.m_factoryId;
    }

    /**
     * @param factory the nodeSetFactory needed to restore a instance of
     *            underlying node
     */
    public void setNodeSetFactory(final NodeSetFactory factory) {
        m_nodeSetFactory = factory;
    }

    /**
     * @return the class of the NodeSetFactory that created this
     *         DynamicNodeTemplate
     */
    public Class<? extends NodeSetFactory> getNodeSetFactoryClass() {
        return m_nodeSetFactory.getClass();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeFactory<? extends NodeModel> createFactoryInstance()
            throws Exception {
        return createFactoryInstance(getFactory(), m_nodeSetFactory,
                m_factoryId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DynamicNodeTemplate)) {
            return false;
        }
        return getID().equals(((DynamicNodeTemplate)obj).getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getID().hashCode();
    }

    public static NodeFactory<? extends NodeModel> createFactoryInstance(
            final Class<? extends NodeFactory<? extends NodeModel>> factoryClass,
            final NodeSetFactory nodeSetFactory, final String factoryId)
            throws InstantiationException, IllegalAccessException,
            InvalidSettingsException {
        NodeFactory<? extends NodeModel> instance = factoryClass.newInstance();
        instance.loadAdditionalFactorySettings(nodeSetFactory
                .getAdditionalSettings(factoryId));
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IRepositoryObject deepCopy() {
        return new DynamicNodeTemplate(this);
    }
}
