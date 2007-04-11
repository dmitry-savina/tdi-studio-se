// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2007 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.core.ui.editor.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.talend.core.model.components.IODataComponent;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.designerproperties.RepositoryToComponentProperty;
import org.talend.core.model.process.EConnectionCategory;
import org.talend.core.model.process.EConnectionType;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.IExternalNode;
import org.talend.core.model.process.INode;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.editor.nodes.Node;

/**
 * DOC nrousseau class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class ChangeValuesFromRepository extends ChangeMetadataCommand {

    private Map<String, Object> oldValues;

    private Element elem;

    private Connection connection;

    private String value;

    private String propertyName;

    private String oldMetadata;

    private Map<String, List<String>> tablesmap;

    private Map<String, List<String>> queriesmap;

    private Map<String, IMetadataTable> repositoryTableMap;

    public ChangeValuesFromRepository(Element elem, Connection connection, String propertyName, String value) {
        this.elem = elem;
        this.connection = connection;
        this.value = value;
        this.propertyName = propertyName;
        oldValues = new HashMap<String, Object>();

        setLabel(Messages.getString("PropertyChangeCommand.Label")); //$NON-NLS-1$
    }

    @Override
    public void execute() {
        // Force redraw of Commponents propoerties
        elem.setPropertyValue(EParameterName.UPDATE_COMPONENTS.getName(), new Boolean(true));

        if (propertyName.equals(EParameterName.PROPERTY_TYPE.getName()) && (EmfComponent.BUILTIN.equals(value))) {
            for (IElementParameter param : elem.getElementParameters()) {
                param.setRepositoryValueUsed(false);
            }
        } else {
            oldValues.clear();
            for (IElementParameter param : elem.getElementParameters()) {
                String repositoryValue = param.getRepositoryValue();
                if (param.isShow(elem.getElementParameters()) && (repositoryValue != null)
                        && (!param.getName().equals(EParameterName.PROPERTY_TYPE.getName()))) {
                    Object objectValue = (Object) RepositoryToComponentProperty.getValue(connection, repositoryValue);
                    if (objectValue != null) {
                        oldValues.put(param.getName(), param.getValue());

                        if (param.getField().equals(EParameterFieldType.CLOSED_LIST) && param.getRepositoryValue().equals("TYPE")) {
                            boolean found = false;
                            String[] list = param.getListRepositoryItems();
                            for (int i = 0; (i < list.length) && (!found); i++) {
                                if (objectValue.equals(list[i])) {
                                    found = true;
                                    elem.setPropertyValue(param.getName(), param.getListItemsValue()[i]);
                                }
                            }
                        } else {
                            elem.setPropertyValue(param.getName(), objectValue);
                        }
                        param.setRepositoryValueUsed(true);
                    } else {
                        if (param.getField().equals(EParameterFieldType.TABLE)
                                && param.getRepositoryValue().equals("XML_MAPPING")) { //$NON-NLS-1$

                            List<Map<String, Object>> table = (List<Map<String, Object>>) elem.getPropertyValue(param.getName());
                            IMetadataTable metaTable = ((Node) elem).getMetadataList().get(0);
                            RepositoryToComponentProperty.getTableXmlFileValue(connection, "XML_MAPPING", param, //$NON-NLS-1$
                                    table, metaTable);
                            param.setRepositoryValueUsed(true);
                        }
                    }
                }
            }
        }
        if (propertyName.equals(EParameterName.PROPERTY_TYPE.getName())) {
            elem.setPropertyValue(EParameterName.PROPERTY_TYPE.getName(), value);

            setOtherProperties();

        } else {
            oldMetadata = (String) elem.getPropertyValue(EParameterName.REPOSITORY_PROPERTY_TYPE.getName());
            elem.setPropertyValue(EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), value);
            if (elem instanceof Node) {
                Node node = (Node) elem;
                boolean metadataInput = false;
                if (node.getCurrentActiveLinksNbInput(EConnectionType.FLOW_MAIN) > 0
                        || node.getCurrentActiveLinksNbInput(EConnectionType.FLOW_REF) > 0
                        || node.getCurrentActiveLinksNbInput(EConnectionType.TABLE) > 0) {
                    metadataInput = true;
                }
                if (!metadataInput) {
                    elem.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.REPOSITORY);
                    IElementParameter repositorySchemaTypeParameter = elem
                            .getElementParameter(EParameterName.REPOSITORY_SCHEMA_TYPE.getName());
                    String repositoryValue = (String) repositorySchemaTypeParameter.getValue();
                    IMetadataTable table = (IMetadataTable) repositoryTableMap.get(repositoryValue);
                    if (table != null) {
                        table = table.clone();
                        table.setTableName(node.getMetadataList().get(0).getTableName());
                        if (!table.sameMetadataAs(node.getMetadataList().get(0))) {
                            ChangeMetadataCommand cmd = new ChangeMetadataCommand(node, null, table);
                            cmd.setRepositoryMode(true);
                            cmd.execute();
                        }
                    }
                }
            }
            elem.setPropertyValue(EParameterName.QUERYSTORE_TYPE.getName(), EmfComponent.REPOSITORY);
            if (queriesmap == null || queriesmap.get(value).isEmpty()) {
                elem.setPropertyValue(EParameterName.QUERYSTORE_TYPE.getName(), EmfComponent.BUILTIN);
            }
            if (tablesmap == null || tablesmap.get(value).isEmpty()) {
                elem.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.BUILTIN);
            }
        }

        refreshPropertyView();
    }

    /**
     * qzhang Comment method "setOtherProperties".
     */
    private void setOtherProperties() {
        boolean metadataInput = false;
        if (elem instanceof Node) {
            Node node = (Node) elem;
            if (node.getCurrentActiveLinksNbInput(EConnectionType.FLOW_MAIN) > 0
                    || node.getCurrentActiveLinksNbInput(EConnectionType.FLOW_REF) > 0
                    || node.getCurrentActiveLinksNbInput(EConnectionType.TABLE) > 0) {
                metadataInput = true;
            }
            if (value.equals(EmfComponent.BUILTIN)) {
                if (!metadataInput) {
                    elem.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), value);
                }
                elem.setPropertyValue(EParameterName.QUERYSTORE_TYPE.getName(), value);
            } else {
                if (!metadataInput) {
                    if (tablesmap != null
                            && !tablesmap.get(elem.getPropertyValue(EParameterName.REPOSITORY_PROPERTY_TYPE.getName())).isEmpty()) {
                        elem.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), value);
                        IElementParameter repositorySchemaTypeParameter = elem
                                .getElementParameter(EParameterName.REPOSITORY_SCHEMA_TYPE.getName());
                        String repositoryValue = (String) repositorySchemaTypeParameter.getValue();
                        IMetadataTable table = (IMetadataTable) repositoryTableMap.get(repositoryValue);
                        if (table != null) {
                            table = table.clone();
                            table.setTableName(node.getMetadataList().get(0).getTableName());
                            if (!table.sameMetadataAs(node.getMetadataList().get(0))) {
                                ChangeMetadataCommand cmd = new ChangeMetadataCommand(node, null, table);
                                cmd.setRepositoryMode(true);
                                cmd.execute();
                            }
                        }
                    }
                } else {
                    Node sourceNode = getRealSourceNode((Node) elem);
                    if (sourceNode != null) {
                        IMetadataTable sourceMetadataTable = sourceNode.getMetadataList().get(0);
                        boolean isExternal = sourceNode.getPluginFullName() != null
                                && !"".equals(sourceNode.getPluginFullName()) && sourceNode.getExternalNode() != null;
                        if (!isExternal && getTake()) {
                            ChangeMetadataCommand cmd = new ChangeMetadataCommand((Node) elem, null, sourceMetadataTable);
                            cmd.execute(true);
                            elem.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), sourceNode
                                    .getPropertyValue(EParameterName.SCHEMA_TYPE.getName()));
                            if (sourceNode.getPropertyValue(EParameterName.SCHEMA_TYPE.getName()).equals(EmfComponent.REPOSITORY)) {
                                elem.setPropertyValue(EParameterName.REPOSITORY_SCHEMA_TYPE.getName(), sourceNode
                                        .getPropertyValue(EParameterName.REPOSITORY_SCHEMA_TYPE.getName()));
                            }
                        }
                    }
                }
            }
            if (queriesmap != null
                    && !queriesmap.get(elem.getPropertyValue(EParameterName.REPOSITORY_PROPERTY_TYPE.getName())).isEmpty()) {
                elem.setPropertyValue(EParameterName.QUERYSTORE_TYPE.getName(), value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected Node getRealSourceNode(INode target) {
        Node sourceNode = null;
        IODataComponent input = null;
        List<org.talend.designer.core.ui.editor.connections.Connection> incomingConnections = null;
        incomingConnections = (List<org.talend.designer.core.ui.editor.connections.Connection>) target.getIncomingConnections();
        for (org.talend.designer.core.ui.editor.connections.Connection connec : incomingConnections) {
            if (connec.isActivate() && connec.getLineStyle().getCategory().equals(EConnectionCategory.MAIN)) {
                input = new IODataComponent(connec);
            }
        }
        if (input != null) {
            INode source = input.getSource();
            if (source instanceof Node) {
                sourceNode = (Node) source;
                // final IExternalNode externalNode = sourceNode.getExternalNode();
                // if (sourceNode.getPluginFullName() != null && !"".equals(sourceNode.getPluginFullName())
                // && sourceNode.getExternalNode() != null) {
                // return getRealSourceNode(externalNode);
                // }
            }
        }
        return sourceNode;
    }

    /**
     * qzhang Comment method "getTake".
     * 
     * @return
     */
    private boolean getTake() {
        Boolean take = MessageDialog.openQuestion(new Shell(), Messages
                .getString("ChangeValuesFromRepository.messageDialog.take"), Messages
                .getString("ChangeValuesFromRepository.messageDialog.takeMessage"));
        return take;
    }

    @Override
    public void undo() {
        // Force redraw of Commponents propoerties
        elem.setPropertyValue(EParameterName.UPDATE_COMPONENTS.getName(), new Boolean(true));

        if (propertyName.equals(EParameterName.PROPERTY_TYPE.getName()) && (EmfComponent.BUILTIN.equals(value))) {
            for (IElementParameter param : elem.getElementParameters()) {
                String repositoryValue = param.getRepositoryValue();
                if (param.isShow(elem.getElementParameters()) && (repositoryValue != null)
                        && (!param.getName().equals(EParameterName.PROPERTY_TYPE.getName()))) {
                    param.setRepositoryValueUsed(true);
                }
            }
        } else {
            for (IElementParameter param : elem.getElementParameters()) {
                String repositoryValue = param.getRepositoryValue();
                if (param.isShow(elem.getElementParameters()) && (repositoryValue != null)) {
                    Object objectValue = (Object) RepositoryToComponentProperty.getValue(connection, repositoryValue);
                    if (objectValue != null) {
                        elem.setPropertyValue(param.getName(), oldValues.get(param.getName()));
                        param.setRepositoryValueUsed(false);
                    }
                }
            }
        }
        if (propertyName.equals(EParameterName.PROPERTY_TYPE.getName())) {
            if (value.equals(EmfComponent.BUILTIN)) {
                elem.setPropertyValue(EParameterName.PROPERTY_TYPE.getName(), EmfComponent.REPOSITORY);
            } else {
                elem.setPropertyValue(EParameterName.PROPERTY_TYPE.getName(), EmfComponent.BUILTIN);
                elem.setPropertyValue(EParameterName.SCHEMA_TYPE.getName(), EmfComponent.BUILTIN);
                elem.setPropertyValue(EParameterName.QUERYSTORE_TYPE.getName(), EmfComponent.BUILTIN);
            }
        } else {
            elem.setPropertyValue(EParameterName.REPOSITORY_PROPERTY_TYPE.getName(), oldMetadata);
        }

        refreshPropertyView();
    }

    /**
     * Sets a sets of maps.
     * 
     * @param tablesmap
     * @param queriesmap
     * @param repositoryTableMap
     */
    public void setMaps(Map<String, List<String>> tablesmap, Map<String, List<String>> queriesmap,
            Map<String, IMetadataTable> repositoryTableMap) {
        this.tablesmap = tablesmap;
        this.queriesmap = queriesmap;
        this.repositoryTableMap = repositoryTableMap;
    }
}
