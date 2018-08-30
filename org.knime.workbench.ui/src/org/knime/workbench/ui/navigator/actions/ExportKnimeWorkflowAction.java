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
 *   20.10.2006 (sieb): created
 */
package org.knime.workbench.ui.navigator.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.wizards.export.WorkflowExportWizard;

/**
 * Action to invoke the knime export wizard.
 *
 * @author Christoph Sieb, University of Konstanz
 * 
 * @deprecated since AP 3.0
 */
@Deprecated
public class ExportKnimeWorkflowAction extends Action {

    private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;

    private static final ImageDescriptor ICON
        = KNIMEUIPlugin.imageDescriptorFromPlugin(
                KNIMEUIPlugin.PLUGIN_ID, "icons/knime_export.png");

    /**
     * The id for this action.
     */
    public static final String ID = "KNIMEExport";

    /**
     * The workbench window; or <code>null</code> if this action has been
     * <code>dispose</code>d.
     */

    /**
     * Create a new instance of this class.
     *
     * @param window the window
     */
    public ExportKnimeWorkflowAction(final IWorkbenchWindow window) {
        super("Export KNIME workflow...");
        if (window == null) {
            throw new IllegalArgumentException();
        }
        setToolTipText("Exports a KNIME workflow to an archive");
        setId(ID); //$NON-NLS-1$
    }

    /**
     * Create a new instance of this class.
     *
     * @param workbench the workbench
     * @deprecated use the constructor
     *             <code>ImportResourcesAction(IWorkbenchWindow)</code>
     */
    @Deprecated
    public ExportKnimeWorkflowAction(final IWorkbench workbench) {
        this(workbench.getActiveWorkbenchWindow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ICON;
    }

    /**
     * Invoke the Import wizards selection Wizard.
     */
    @Override
    public void run() {
        IWorkbenchWindow workbenchWindow =
            PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            // action has been disposed
            return;
        }

        WorkflowExportWizard wizard = new WorkflowExportWizard();
        IStructuredSelection selectionToPass;
        // get the current workbench selection
        ISelection workbenchSelection =
                workbenchWindow.getSelectionService().getSelection();
        if (workbenchSelection instanceof IStructuredSelection) {
            selectionToPass = (IStructuredSelection)workbenchSelection;
        } else {
            selectionToPass = StructuredSelection.EMPTY;
        }

        wizard.init(workbenchWindow.getWorkbench(), selectionToPass);

        Shell parent = workbenchWindow.getShell();
        WizardDialog dialog = new WizardDialog(parent, wizard);
        dialog.create();
        dialog.getShell().setSize(
                Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
                SIZING_WIZARD_HEIGHT);
        dialog.open();
    }

}
