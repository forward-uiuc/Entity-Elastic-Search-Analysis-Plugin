#!/usr/bin/env bash
mvn package -DskipTests
cp target/entity-elastic-search-analysis-plugin-5.6.1.jar /Users/longpham/Workspace/elasticsearch-5.6.1/plugins/entity-elastic-search-analysis-plugin