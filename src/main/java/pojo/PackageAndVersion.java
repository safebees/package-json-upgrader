package pojo;

public class PackageAndVersion {
    private final String packageName;
    private final String version;
    private final boolean devDependency;

    public String newVersion;
    public boolean delete;

    public PackageAndVersion(String packageName, String version, boolean devDependency) {
        this.packageName = packageName;
        this.version = version;
        this.devDependency = devDependency;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getVersion() {
        return version;
    }

    public boolean isDevDependency() {
        return devDependency;
    }
}
