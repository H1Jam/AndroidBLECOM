![workflow](https://github.com/H1Jam/EzBlueLib/actions/workflows/gradle.yml/badge.svg)
[![JitPack](https://jitpack.io/v/H1Jam/EzBlueLib.svg)](https://jitpack.io/#H1Jam/EzBlueLib)
![license](https://img.shields.io/github/license/H1Jam/EzBlueLib)
# EzBlueLib
An android library for Bluetooth serial communication.

![Modems](/docs/blemodems2.jpg)

## Download 
The library has been published to JitPack. In order to add the dependency you have to follow these steps:

1. For gradle build tools < 7.x.x add this in your root build.gradle at the end of repositories:

   ```javascript
   allprojects {
     repositories {
       ...
       maven { url "https://jitpack.io" }
     }
   }
   ```
	As of the 7.X.X gradle build tools you have to do this instead:
	
	```dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			...
			maven { url "https://jitpack.io" }
		}
	}```
	
2. Add the dependency

     ```javascript
     dependencies {
		...
		implementation 'com.github.H1Jam:EzBlueLib:1.0.4'
     }
     ```
	 * Chose the latest version or the one that works for you.
 
3. Add permission in `AndroidManifest.xml` 

```xml
  <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```
