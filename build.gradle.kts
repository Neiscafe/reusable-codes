defaultConfig {
        applicationId = ""
        minSdk = 24
        //warning desabilitado pois não utilizamos a app store
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val prop = Properties().apply {
                load(FileInputStream(File(rootProject.rootDir, "local.properties")))
            }
            buildConfigField("boolean", "f+debug", "true")
            buildConfigField("String", "username", prop.getProperty("debug_username") as String)
            buildConfigField("String", "password", prop.getProperty("debug_password") as String)
            isMinifyEnabled = false
        }
        release {
            buildConfigField("boolean", "debug", "false")
            buildConfigField("String", "username", "")
            buildConfigField("String", "password", "")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
