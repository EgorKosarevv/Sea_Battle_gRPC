module org.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.grpc;
    requires io.netty.buffer;
    requires io.grpc.protobuf;
    requires protobuf.java;
    requires io.grpc.stub;
    requires com.google.common;
    requires java.annotation;
    requires java.desktop;
    requires java.sql;
    requires proto.google.common.protos;


    opens org.example.demo to javafx.fxml;
    exports org.example.demo;
    exports client;
    opens client to javafx.fxml;
//    opens org.example.demo.client to javafx.fxml;
}