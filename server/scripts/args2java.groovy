def targetPackage = "net.apnic.rpki.server"
def targetClass = "Arguments"

def targetBase = new File(project.build.directory, "generated-sources")
project.addCompileSourceRoot(targetBase.path)

def targetPackageDir = new File(targetBase, targetPackage.replace(".", File.separator))
println "Creating generated source directory:" + targetPackageDir

targetPackageDir.mkdirs()

def target = new File(targetPackageDir, "${targetClass}.java")

if (target.exists()) {
    target.delete()
}

target.createNewFile()

target << "package ${targetPackage};\n\n"
target << "import org.apache.commons.cli.OptionBuilder;\n"
target << "import org.apache.commons.cli.Options;\n\n"
target << "class ${targetClass} {\n"
target << "    @SuppressWarnings(\"static-access\")"
target << "    private final static Options options = new Options()"

class UnknownPoptType extends Exception {}

new File("${project.properties.getProperty("args.source")}").eachLine() { line ->
    line = line.replaceAll(/^.*\{/, "")
    line = line.replaceAll(/\}.*$/, "")
    def (longName, shortName, info, ptr, value, desc, argDesc) = line.split(/,\s*/)
    target << "\n                .addOption(OptionBuilder"
    if (longName != "0") { target << ".withLongOpt(${longName})" }

    def argName = '"' + ptr.substring(1) + '"';
    if (argName == "\"\"") argName = longName;
    if (argName == "0") argName = shortName.replaceAll(/'/, "\"");
    argName = argName.replaceAll(/-/, "_");
    if (argName == "0") argName = "null";

    switch (info) {
        case "POPT_ARG_NONE":
            target << ".withArgName(${argName})"
            break
        case ~/POPT_ARG_(STRING|INT|LONG|FLOAT|DOUBLE)/:
            target << ".hasArg().withArgName(${argName})"
            break
        case "POPT_ARG_VAL":
        case "POPT_BIT_SET":
            target << ".withArgName(\"${ptr.substring(1)}\")"
            break;
        default:
            throw new UnknownPoptType()
    }
    target << ".withDescription(\"${value}\")"

    if (shortName == "0") {
        target << ".create())"
    } else {
        target << ".create($shortName))"
    }
}

target << ";\n\n"
target << "    public static Options rsyncOptions() { return options; }\n\n"
target << "}\n"
