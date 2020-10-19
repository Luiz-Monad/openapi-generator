"/mnt/hgfs/extern/lab/openapi-generator/mvnw" install -f "/mnt/hgfs/extern/lab/openapi-generator/modules/openapi-generator-core/pom.xml" -Djacoco.skip=true -Dmaven.test.skip=true
"/mnt/hgfs/extern/lab/openapi-generator/mvnw" install -f "/mnt/hgfs/extern/lab/openapi-generator/modules/openapi-generator/pom.xml" -Djacoco.skip=true -Dmaven.test.skip=true
"/mnt/hgfs/extern/lab/openapi-generator/mvnw" install -f "/mnt/hgfs/extern/lab/openapi-generator/modules/openapi-generator-cli/pom.xml" -Djacoco.skip=true -Dmaven.test.skip=true
cp modules/openapi-generator-cli/target/openapi-generator-cli.jar /mnt/hgfs/extern/workbench/parental-control/languagefilter/api/
java -jar modules/openapi-generator-cli/target/openapi-generator-cli.jar list

