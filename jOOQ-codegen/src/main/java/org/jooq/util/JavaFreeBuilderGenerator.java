package org.jooq.util;

import org.jooq.tools.StringUtils;

import java.util.List;

import static org.jooq.tools.StringUtils.defaultIfBlank;

/**
 * Add FreeBuilder code to JOOQ generated interfaces so Builder's can be generated.
 * https://github.com/google/FreeBuilder
 *
 * Adds: <pre>
 * {@code
 * import org.inferred.freebuilder.FreeBuilder;
 * @FreeBuilder
 * class Builder extends InterfaceName_Builder {}
 * }
 * </pre>
 *
 * To use add/change the generator name tag to org.jooq.util.JavaFreeBuilderGenerator
 * 
 * Created by eghm on 3/30/16.
 */
public class JavaFreeBuilderGenerator extends JavaGenerator {

    @Override
    protected void generateInterface(TableDefinition table) {
        JavaWriter out = newJavaWriter(getStrategy().getFile(table, GeneratorStrategy.Mode.INTERFACE));
        log.info("Generating interface", out.file().getName());
        generateFreeBuilderInterface(table, out);
        closeJavaWriter(out);
    }

    /* Copied from JavaGenerator generateInterface(Definition tableOrUDT, JavaWriter out) */
    private void generateFreeBuilderInterface(Definition tableOrUDT, JavaWriter out) {
        final String className = getStrategy().getJavaClassName(tableOrUDT, GeneratorStrategy.Mode.INTERFACE);
        final List<String> interfaces = out.ref(getStrategy().getJavaClassImplements(tableOrUDT, GeneratorStrategy.Mode.INTERFACE));

        printPackage(out, tableOrUDT, GeneratorStrategy.Mode.INTERFACE);

        if (tableOrUDT instanceof TableDefinition)
            generateInterfaceClassJavadoc((TableDefinition) tableOrUDT, out);
        else
            generateUDTInterfaceClassJavadoc((UDTDefinition) tableOrUDT, out);

        printClassAnnotations(out, tableOrUDT.getSchema());

        if (tableOrUDT instanceof TableDefinition)
            printTableJPAAnnotation(out, (TableDefinition) tableOrUDT);

        if (scala)
            out.println("trait %s [[before=extends ][%s]] {", className, interfaces);
        else {
            out.println("@%s", out.ref("org.inferred.freebuilder.FreeBuilder"));
            out.println("public interface %s [[before=extends ][%s]] {", className, interfaces);
            out.println("");
            out.tab(1).println("class Builder extends %s_Builder {}", className);
        }

        for (TypedElementDefinition<?> column : getTypedElements(tableOrUDT)) {
            final String comment = StringUtils.defaultString(column.getComment());
            final String setterReturnType = fluentSetters() ? className : "void";
            final String setter = getStrategy().getJavaSetterName(column, GeneratorStrategy.Mode.INTERFACE);
            final String getter = getStrategy().getJavaGetterName(column, GeneratorStrategy.Mode.INTERFACE);
            final String type = out.ref(getJavaType(column.getType(), GeneratorStrategy.Mode.INTERFACE));
            final String name = column.getQualifiedOutputName();

            if (!generateImmutablePojos()) {
                out.tab(1).javadoc("Setter for <code>%s</code>.%s", name, defaultIfBlank(" " + comment, ""));

                if (scala)
                    out.tab(1).println("def %s(value : %s) : %s", setter, type, setterReturnType);
                else
                    out.tab(1).println("public %s %s(%s value);", setterReturnType, setter, type);
            }

            out.tab(1).javadoc("Getter for <code>%s</code>.%s", name, defaultIfBlank(" " + comment, ""));

            if (column instanceof ColumnDefinition)
                printColumnJPAAnnotation(out, (ColumnDefinition) column);

            printValidationAnnotation(out, column);

            if (scala)
                out.tab(1).println("def %s : %s", getter, type);
            else
                out.tab(1).println("public %s %s();", type, getter);
        }

        if (!generateImmutablePojos()) {
            String local = getStrategy().getJavaClassName(tableOrUDT, GeneratorStrategy.Mode.INTERFACE);
            String qualified = out.ref(getStrategy().getFullJavaClassName(tableOrUDT, GeneratorStrategy.Mode.INTERFACE));

            out.tab(1).header("FROM and INTO");

            out.tab(1).javadoc("Load data from another generated Record/POJO implementing the common interface %s", local);

            if (scala)
                out.tab(1).println("def from(from : %s)", qualified);
            else
                out.tab(1).println("public void from(%s from);", qualified);

            out.tab(1).javadoc("Copy data into another generated Record/POJO implementing the common interface %s", local);

            if (scala)
                out.tab(1).println("def into [E <: %s](into : E) : E", qualified);
            else
                out.tab(1).println("public <E extends %s> E into(E into);", qualified);
        }


        if (tableOrUDT instanceof TableDefinition)
            generateInterfaceClassFooter((TableDefinition) tableOrUDT, out);
        else
            generateUDTInterfaceClassFooter((UDTDefinition) tableOrUDT, out);

        out.println("}");
    }
}
