module com.github.robtimus.servlet {
    requires transitive java.servlet;
    requires transitive com.github.robtimus.io.functions;
    requires org.slf4j;

    exports com.github.robtimus.servlet;
    exports com.github.robtimus.servlet.http;
    exports com.github.robtimus.servlet.parameters;
}
