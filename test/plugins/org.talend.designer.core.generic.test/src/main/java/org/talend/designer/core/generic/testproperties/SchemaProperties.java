package org.talend.designer.core.generic.testproperties;

import static org.talend.daikon.properties.PropertyFactory.*;
import static org.talend.daikon.properties.presentation.Widget.*;

import org.apache.avro.SchemaBuilder;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.daikon.properties.Property;
import org.talend.daikon.properties.Property.Type;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget.WidgetType;

/**
 * created by ycbai on 2016年3月15日 Detailled comment
 * <p>
 * Only for test which is copied from org.talend.components.common.SchemaProperties
 *
 */
public class SchemaProperties extends ComponentProperties {

    /** An empty schema is used for an uninitialized SchemaProperties. */
    public static final String EMPTY_SCHEMA = SchemaBuilder.builder().record("EmptyRecord").fields().endRecord().toString(); //$NON-NLS-1$

    public SchemaProperties(String name) {
        super(name);
    }

    //
    // Properties
    //
    public Property schema = newProperty(Type.SCHEMA, "schema"); //$NON-NLS-1$

    @Override
    public void setupProperties() {
        super.setupProperties();
        schema.setValue(EMPTY_SCHEMA);
    }

    @Override
    public void setupLayout() {
        super.setupLayout();

        Form schemaForm = Form.create(this, Form.MAIN, "Schema"); //$NON-NLS-1$
        schemaForm.addRow(widget(schema).setWidgetType(WidgetType.SCHEMA_EDITOR));

        Form schemaRefForm = Form.create(this, Form.REFERENCE, "Schema"); //$NON-NLS-1$
        schemaRefForm.addRow(widget(schema).setWidgetType(WidgetType.SCHEMA_REFERENCE));
    }

}