/*
 * JBoss, Home of Professional Open Source.
 *
 * See the LEGAL.txt file distributed with this work for information regarding copyright ownership and licensing.
 *
 * See the AUTHORS.txt file distributed with this work for a full listing of individual contributors.
 */
package org.komodo.eclipse.runtime.client.database;

import java.util.HashMap;
import org.eclipse.datatools.sqltools.core.services.SQLEditorUIService;
import org.eclipse.datatools.sqltools.editor.ui.core.SQLDevToolsUIConfiguration;

/**
 * @since 8.0
 */
public class TeiidDBUIConfiguration extends SQLDevToolsUIConfiguration {

    @Override
    public SQLEditorUIService getSQLEditorUIService() {
        return new SQLEditorUIService() {
            @Override
            public HashMap getAdditionalActions() {
                HashMap additions = super.getAdditionalActions();
// TODO
//                additions.put("", new TeiidExplainSQLActionDelegate()); //$NON-NLS-1$
                return additions;
            }
        };
    }

}
