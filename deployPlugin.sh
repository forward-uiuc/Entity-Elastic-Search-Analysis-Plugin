#!/usr/bin/env bash
mvn package -DskipTests
/Users/longpham/Workspace/elasticsearch-5.6.1/bin/elasticsearch-plugin remove entity-elastic-search-analysis-plugin
/Users/longpham/Workspace/elasticsearch-5.6.1/bin/elasticsearch-plugin install file:///Users/longpham/Workspace/Entity-Elastic-Search-Analysis-Plugin/target/releases/entity-elastic-search-analysis-plugin-5.6.1.zip#cp plugin-security.policy /Users/longpham/Workspace/elasticsearch-5.6.1/plugins/entity-elastic-search-analysis-plugin/
