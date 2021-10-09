import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import pojo.PackageAndVersion;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    private static final String PACKAGE_JSON_V1 =
            "package_intern_v1.json";
    private static final String PACKAGE_JSON_V2 =
            "package_intern_v2.json";
    private static final String PACKAGE_JSON_DESTINATION =
            "package_app.json";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DEV_DEPENDENCIES = "devDependencies";

    public static void main(String[] args) throws IOException {

        Map<String, PackageAndVersion> appDeps = calculateDeps(List.of(PACKAGE_JSON_DESTINATION));

        // todo according if appDeps contains intern / extern load a different .json

        Map<String, PackageAndVersion> depsOld = calculateDeps(List.of(PACKAGE_JSON_V1));
        Map<String, PackageAndVersion> depsNew = calculateDeps(List.of(PACKAGE_JSON_V2));

        ArrayList<PackageAndVersion> newDeps = new ArrayList<>();

        for (PackageAndVersion currentPackage : appDeps.values()) {
            PackageAndVersion oldPackage = depsOld.get(getKey(currentPackage));
            PackageAndVersion newPackage = depsNew.get(getKey(currentPackage));
            if (oldPackage == null && newPackage == null) {
                System.out.println(currentPackage.getPackageName() + " is not under our control please update it yourself");
                newDeps.add(currentPackage);
            } else if (oldPackage != null && newPackage == null) {
                currentPackage.delete = true;
                System.out.println(currentPackage.getPackageName() + " was dropped");
            } else {
                currentPackage.newVersion = newPackage.getVersion();
                System.out.println(currentPackage.getPackageName() + " was updated from " + currentPackage.getVersion() + " -> " + newPackage.getVersion());
                newDeps.add(newPackage);
            }
        }

        for (PackageAndVersion newPackage : depsNew.values()) {
            PackageAndVersion found = appDeps.get(getKey(newPackage));
            if (found == null) {
                System.out.println(newPackage.getPackageName() + " version " + newPackage.getVersion() + "was added");
                newDeps.add(newPackage);
            }
        }

        Map<Object, Object> fileAsMap = getFileAsMap(PACKAGE_JSON_DESTINATION);

        fileAsMap.put(DEV_DEPENDENCIES, newDeps.stream().filter(PackageAndVersion::isDevDependency)
                .collect(Collectors.toMap(PackageAndVersion::getPackageName, PackageAndVersion::getVersion)));

        fileAsMap.put(DEPENDENCIES, newDeps.stream().filter(s -> !s.isDevDependency())
                .collect(Collectors.toMap(PackageAndVersion::getPackageName, PackageAndVersion::getVersion)));


        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String json = gson.toJson(fileAsMap);

        System.out.println("json = " + json);
        // todo override to package_app.json

    }

    private static Map<String, PackageAndVersion> calculateDeps(List<String> files) throws IOException {

        HashMap<String, PackageAndVersion> allPackages = new HashMap<>();
        for (String file : files) {

            Map<?, ?> map = getFileAsMap(file);

            aggregatePackageAndVersion(map.get(DEPENDENCIES), allPackages, false);
            aggregatePackageAndVersion(map.get(DEV_DEPENDENCIES), allPackages, true);
        }
        return allPackages;
    }

    private static Map<Object, Object> getFileAsMap(String file) throws IOException {
        Gson gson = new Gson();
        Reader reader = Files.newBufferedReader(Paths.get(file));
        //noinspection unchecked
        Map<Object, Object> map = gson.fromJson(reader, Map.class);
        reader.close();
        return map;
    }

    private static void
    aggregatePackageAndVersion(Object entry, HashMap<String, PackageAndVersion> deps, boolean dev) {
        if (entry instanceof LinkedTreeMap) {
            LinkedTreeMap<?, ?> value = (LinkedTreeMap<?, ?>) entry;
            for (Object packageName : value.keySet()) {
                Object version = value.get(packageName);
                if (version instanceof String && packageName instanceof String) {
                    PackageAndVersion packageAndVersion = new PackageAndVersion((String) packageName, (String) version, dev);
                    deps.put(getKey(packageAndVersion), packageAndVersion);
                }
            }
        }
    }

    private static String getKey(PackageAndVersion packageAndVersion) {
        return packageAndVersion.getPackageName() + (packageAndVersion.isDevDependency() ? "dev" : "");
    }
}
