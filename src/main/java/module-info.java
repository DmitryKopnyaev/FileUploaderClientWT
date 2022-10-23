module com.dimentor {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires java.rmi;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires zip4j;
    requires org.apache.commons.io;
    requires org.apache.commons.codec;
//    requires org.apache.commons.lang3;

    opens com.dimentor to javafx.fxml;
    exports com.dimentor;
    exports com.dimentor.util;
    exports com.dimentor.repository;
}
