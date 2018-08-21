#!/usr/bin/env bash
mvn package -DskipTests
/Users/longpham/Workspace/ForwardSearchInstallationGuide/elasticsearch-5.6.1/bin/elasticsearch-plugin remove entity-elastic-search-analysis-plugin
/Users/longpham/Workspace/ForwardSearchInstallationGuide/elasticsearch-5.6.1/bin/elasticsearch-plugin install file:///Users/longpham/Workspace/ForwardSearchInstallationGuide/Entity-Elastic-Search-Analysis-Plugin/target/releases/entity-elastic-search-analysis-plugin-5.6.1.zip
