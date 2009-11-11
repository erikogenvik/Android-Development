/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.properties;

import java.util.Map;
import com.android.sdklib.internal.project.ApkSettings;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.PropertyPage;

import com.android.ide.eclipse.adt.AdtPlugin;
import com.android.ide.eclipse.adt.internal.sdk.Sdk;
import com.android.sdklib.IAndroidTarget;
import com.android.sdkuilib.internal.widgets.ApkConfigWidget;
import com.android.sdkuilib.internal.widgets.SdkTargetSelector;
/**
 * Property page for "Android" project.
 * This is accessible from the Package Explorer when right clicking a project and choosing
 * "Properties".
 *
 */
public class AndroidPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {

	private IProject mProject;
    private SdkTargetSelector mSelector;
    // APK-SPLIT: This is not yet supported, so we hide the UI
//    private Button mSplitByDensity;

    public AndroidPropertyPage() {
        // pass
    }

    @Override
    protected Control createContents(Composite parent) {
        // get the element (this is not yet valid in the constructor).
        mProject = (IProject)getElement();

        // get the targets from the sdk
        IAndroidTarget[] targets = null;
        if (Sdk.getCurrent() != null) {
            targets = Sdk.getCurrent().getTargets();
        }

        // build the UI.
        Composite top = new Composite(parent, SWT.NONE);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        top.setLayout(new GridLayout(1, false));

        Label l = new Label(top, SWT.NONE);
        l.setText("Project Build Target");

        mSelector = new SdkTargetSelector(top, targets);

        /*
         * APK-SPLIT: This is not yet supported, so we hide the UI
        Group g = new Group(top, SWT.NONE);
        g.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        g.setLayout(new GridLayout(1, false));
        g.setText("APK Generation");

        mSplitByDensity = new Button(g, SWT.CHECK);
        mSplitByDensity.setText("One APK per density");

*/
        // fill the ui
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null && mProject.isOpen()) {
            // get the target
            IAndroidTarget target = currentSdk.getTarget(mProject);
            if (target != null) {
                mSelector.setSelection(target);
            }

            /*
             * APK-SPLIT: This is not yet supported, so we hide the UI
            // get the project settings
            ApkSettings settings = currentSdk.getApkSettings(mProject);
            mSplitByDensity.setSelection(settings.isSplitByDpi());
            */
        }
        
        l = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL);
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        //We need a new grid for the StringButtonFieldEditor, since that uses two columns.
        Composite directoryContainer = new Composite(top, SWT.NONE);
        directoryContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
        directoryContainer.setLayout(new GridLayout(1, false));
        
        //The field editor should allow for selection of a container within the current project. I couldn't figure out how to do that, so we'll show the whole workbench for now.
        final StringButtonFieldEditor resDirectoryFieldEditor = new StringButtonFieldEditor("resDirectory", "Resource directory", directoryContainer)
        {

			@Override
			protected String changePressed() {
				ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), mProject.getFolder(AdtPlugin.getResourceLocation(mProject)), false, "");
				dialog.setTitle("Resource location");
				dialog.setMessage("Select the location of the Android resources (it's currently only possible to select a location within the current project):");
				
				dialog.open();
				Object[] results = dialog.getResult();
				if (results != null && results.length > 0) {
					Object firstSelection = results[0];
					if (firstSelection instanceof IPath) {
						//The path is local to the workspace, so we'll have to remove the first part to get the project local path
						IPath resourcePath = (IPath)firstSelection;
						String localPath = resourcePath.toString();
						if (localPath.startsWith("/" + mProject.getName())) {
							return localPath.substring(("/" + mProject.getName()).length());
						} else {
							AdtPlugin.printErrorToConsole("Not a project local path: " + localPath + ".");
						}
					}
				}
				return null;
			}
        };
                
        try {
			resDirectoryFieldEditor.setStringValue(mProject.getPersistentProperty(AdtPlugin.PROP_RES_DIRECTORY));
		}
		catch (CoreException e) {
			AdtPlugin.printErrorToConsole("Error when trying to set resource location property.", e);
		}
        
        resDirectoryFieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent event) {
        		try {
					mProject.setPersistentProperty(AdtPlugin.PROP_RES_DIRECTORY, event.getNewValue().toString());
				}
				catch (CoreException e) {
					AdtPlugin.printErrorToConsole("Error when trying to get resource location property.", e);
				}
			}
		});
      

        mSelector.setSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                // look for the selection and validate the page if there is a selection
                IAndroidTarget target = mSelector.getSelected();
                setValid(target != null);
            }
        });

        if (mProject.isOpen() == false) {
            // disable the ui.
        }

        return top;
    }

    @Override
    public boolean performOk() {
        Sdk currentSdk = Sdk.getCurrent();
        if (currentSdk != null) {
            ApkSettings apkSettings = new ApkSettings();
            /*
             * APK-SPLIT: This is not yet supported, so we hide the UI
            apkSettings.setSplitByDensity(mSplitByDensity.getSelection());
             */

            currentSdk.setProject(mProject, mSelector.getSelected(), apkSettings);
        }

        return true;
    }
}
