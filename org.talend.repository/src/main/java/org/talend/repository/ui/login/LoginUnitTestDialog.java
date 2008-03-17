// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.login;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.MessageBoxExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.ui.swt.dialogs.ErrorDialogWidthDetailArea;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.general.Project;
import org.talend.core.prefs.PreferenceManipulator;
import org.talend.core.ui.branding.IBrandingService;
import org.talend.repository.RepositoryPlugin;
import org.talend.repository.i18n.Messages;
import org.talend.repository.license.LicenseManagement;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.registeruser.RegisterManagement;
import org.talend.repository.ui.wizards.license.LicenseWizard;
import org.talend.repository.ui.wizards.license.LicenseWizardDialog;
import org.talend.repository.ui.wizards.register.RegisterWizard;

/**
 * Login dialog. <br/>
 * 
 * $Id: LoginDialog.java 4146 2007-07-02 09:18:32 +0000 (Mon, 02 Jul 2007) smallet $
 * 
 */
public class LoginUnitTestDialog extends TitleAreaDialog {

    /** The login composite. */
    private LoginUnitTestComposite loginUnitTestComposite;

    /**
     * Construct a new LoginUnitTestDialog.
     * 
     * @param parentShell Parent shell.
     */
    public LoginUnitTestDialog(Shell parentShell) {
        super(parentShell);
        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        ImageDescriptor imgDesc = brandingService.getLoginHImage();
        if (imgDesc != null) {
            setTitleImage(ImageProvider.getImage(imgDesc));
        }
        // RGB rgb = parentShell.getBackground().getRGB();
        RGB rgb = new RGB(255, 255, 255);
        setTitleAreaColor(rgb);
    }

    /**
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        newShell.setText(Messages.getString("LoginDialog.title", brandingService.getFullProductName())); //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(final Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        composite.setBackground(new Color(null, 0, 0, 0));

        Composite container = new Composite(composite, SWT.NONE);

        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        container.setLayout(layout);
        container.setBackground(new Color(null, 0, 0, 0));

        IBrandingService brandingService = (IBrandingService) GlobalServiceRegister.getDefault().getService(
                IBrandingService.class);
        new ImageCanvas(container, brandingService.getLoginVImage()); //$NON-NLS-1$

        try {
            if (!LicenseManagement.isLicenseValidated()) {
                LicenseWizard licenseWizard = new LicenseWizard();
                LicenseWizardDialog dialog = new LicenseWizardDialog(getShell(), licenseWizard);
                dialog.setTitle(Messages.getString("LicenseWizard.windowTitle")); //$NON-NLS-1$
                // if (dialog.open() == WizardDialog.OK) {
                LicenseManagement.acceptLicense();
                // } else {
                // System.exit(0);
                // }
            }
            if (!RegisterManagement.isProductRegistered()) {
                RegisterWizard registerWizard = new RegisterWizard();

                // // set registation information for unit test ////
                registerWizard.setCountry("FR");
                registerWizard.setEmail("testUnitaire@talend.com");
                // /////////////////////////////////////////////////

                // WizardDialog dialog = new RegisterWizardDialog(getShell(), registerWizard);
                // dialog.setTitle(Messages.getString("RegisterWizard.windowTitle")); //$NON-NLS-1$
                // if (dialog.open() == WizardDialog.OK) {
                // project language
                RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                        Context.REPOSITORY_CONTEXT_KEY);
                ECodeLanguage codeLanguage = repositoryContext.getProject().getLanguage();
                String projectLanguage = codeLanguage.getName();

                // OS
                String osName = System.getProperty("os.name");
                String osVersion = System.getProperty("os.version");

                // Java version
                String javaVersion = System.getProperty("java.version");

                // Java Memory
                long totalMemory = Runtime.getRuntime().totalMemory();

                // RAM
                com.sun.management.OperatingSystemMXBean composantSystem = (com.sun.management.OperatingSystemMXBean) ManagementFactory
                        .getOperatingSystemMXBean();
                Long memRAM = new Long(composantSystem.getTotalPhysicalMemorySize() / 1024);

                // CPU
                int nbProc = Runtime.getRuntime().availableProcessors();

                RegisterManagement.register(registerWizard.getEmail(), registerWizard.getCountry(), registerWizard
                        .isProxyEnabled(), registerWizard.getProxyHost(), registerWizard.getProxyPort(),
                        org.talend.core.CorePlugin.getDefault().getBundle().getHeaders().get(
                                org.osgi.framework.Constants.BUNDLE_VERSION).toString(), projectLanguage, osName, osVersion,
                        javaVersion, totalMemory, memRAM, nbProc);
                // } else {
                // RegisterManagement.decrementTry();
                // }
            }
        } catch (BusinessException e) {
            ErrorDialogWidthDetailArea errorDialog = new ErrorDialogWidthDetailArea(getShell(), RepositoryPlugin.PLUGIN_ID,
                    Messages.getString("RegisterWizardPage.serverCommunicationProblem"), e.getMessage()); //$NON-NLS-1$
        }

        loginUnitTestComposite = new LoginUnitTestComposite(container, SWT.NONE, this);
        GridData data = new GridData(GridData.FILL_BOTH);
        data.widthHint = 350;
        loginUnitTestComposite.setLayoutData(data);

        Label titleBarSeparator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
        titleBarSeparator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        return composite;
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        updateButtons();
    }

    /**
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        logIn(loginUnitTestComposite.getProject());
    }

    /**
     * DOC smallet Comment method "logIn".
     * 
     * @param project
     */
    protected void logIn(final Project project) {
        // Save last used parameters
        PreferenceManipulator prefManipulator = new PreferenceManipulator(CorePlugin.getDefault().getPreferenceStore());
        prefManipulator.setLastConnection(loginUnitTestComposite.getConnection().getName());
        prefManipulator.setLastProject(project.getLabel());

        try {
            try {
                IRunnableWithProgress op = new IRunnableWithProgress() {

                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            ProxyRepositoryFactory.getInstance().logOnProject(project, new NullProgressMonitor());
                        } catch (PersistenceException e) {
                            throw new InvocationTargetException(e);
                        } catch (LoginException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                };
                new ProgressMonitorDialog(getShell()).run(true, false, op);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof PersistenceException) {
                    throw (PersistenceException) e.getTargetException();
                }
                if (e.getTargetException() instanceof LoginException) {
                    throw (LoginException) e.getTargetException();
                }
            } catch (InterruptedException e) {
                //
            }
        } catch (PersistenceException e) {
            MessageBoxExceptionHandler.process(e, getShell());
            return;
        } catch (LoginException e) {
            setErrorMessage(e.getMessage());
            return;
        }

        super.okPressed();
    }

    public void updateButtons() {
        getButton(IDialogConstants.OK_ID).setEnabled(loginUnitTestComposite.canFinish());
    }

    /**
     * Canvas displaying an image.<br/>
     * 
     * $Id: LoginUnitTestDialog.java 4146 2007-07-02 09:18:32 +0000 (Mon, 02 Jul 2007) smallet $
     * 
     */
    private class ImageCanvas extends Canvas {

        private Image img;

        public ImageCanvas(Composite parent, ImageDescriptor imgDesc) {
            super(parent, SWT.NONE);

            if (imgDesc != null) {
                img = imgDesc.createImage();

                addPaintListener(new PaintListener() {

                    public void paintControl(PaintEvent e) {
                        e.gc.drawImage(img, 0, 0);
                    }
                });
            }
        }

        /*
         * @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
         */
        @Override
        public Point computeSize(int wHint, int hHint, boolean changed) {
            Point size;
            if (img != null) {
                size = new Point(img.getBounds().width, img.getBounds().height);
            } else {
                size = super.computeSize(wHint, hHint, changed);
            }
            return size;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.swt.widgets.Widget#dispose()
         */
        @Override
        public void dispose() {
            if (img != null) {
                img.dispose();
                img = null;
            }

            super.dispose();
        }

    }

}
