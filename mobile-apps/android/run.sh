#! /bin/bash

BUILD_TIME=$1
whattobuild=$2
supplier_name=$3
buildType=$4
echo "Build Time is:$BUILD_TIME"

CWD=`pwd -P`
HOME_FOLDER=${HOME}
BASE_FOLDER=${HOME}
JAVA_HOME=${BASE_FOLDER}/jdk-9.0.4
CACERTS_FOLDER=${JAVA_HOME}/lib/security
export ANDROID_HOME="${BASE_FOLDER}/android-sdk"
export ANDROID_NDK="${BASE_FOLDER}/android-ndk"
#export ANDROID_SDK_TOOLS_VERSION="4333796"
export ANDROID_SDK_TOOLS_VERSION="6609375"
export ANDROID_NDK_VERSION="r21c"
export TERM=dumb
export ANDROID_SDK_HOME="$ANDROID_HOME"
export ANDROID_NDK_HOME="$ANDROID_NDK/android-ndk-$ANDROID_NDK_VERSION"
export PATH="$PATH:$JAVA_HOME/bin:$ANDROID_SDK_HOME/emulator:$ANDROID_SDK_HOME/cmdline-tools/tools/bin:$ANDROID_SDK_HOME/tools:$ANDROID_SDK_HOME/platform-tools:$ANDROID_NDK"
export CWD=`pwd -P`
setup_gradle() {
    echo "Setting up the gradle environment.."
    appName=$1
    cd ${CWD}/${appname}
    chmod +x gradlew

    #echo "Listing the available tasks to make sure gradle is initialized..."
    ./gradlew tasks
}

build_app() {
    
    appname=$1
    cd ${CWD}
    echo Current working dir for the build:`pwd -P`

    #buildType=$(grep "BUILD=" "build_settings.txt" | awk -F"=" '{print $2}' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')
    echo "Build Type is:${buildType}"
    incVersionCode=$(grep "INCREMENT_VERSION_CODE=" "build_settings.txt" | awk -F"=" '{print $2}' | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

    setup_gradle $appname

    echo "Building the app:${appname}"
    cd ${CWD}/${appname}
    versionName=$(grep "versionName" ./app/build.gradle | awk -F" " '{print $2}' | tr -d \")
    echo "Do we need to increment the version code:${incVersionCode}"
    if [ "$incVersionCode" == "1" ]; then
        echo "Incrementing the version code for this build now"
        prevVersionCode=$(grep "versionCode" ./app/build.gradle | awk -F" " '{print $2}')
        echo "Version Code:${prevVersionCode} Incrementing it now"
        versionCode=$((prevVersionCode+1))
        echo "Incremented version code:${versionCode}"
        sed -i -e "s/versionCode.*${prevVersionCode}$/versionCode ${versionCode}/" ./app/build.gradle
        echo "Updated gradle bundle file is:"
        cat ./app/build.gradle
    fi

    if [ "$buildType" == "Release" ]; then
        echo "Copying the key.."
        cp "${CWD}/techheralds.jks" ./app/
        echo "Copied the jks file for the release build"
        ls -alh ./app/
    fi

    echo "Decoding the google-services.json details...from file:google-services-${buildType}.json"
    cat google-services-${buildType}.json | base64 --decode > ./app/google-services.json
    
    #echo "Decoded content is:"
    #cat ./app/google-services.json

    ./gradlew assemble${buildType}

    echo "Builing is complete..."
    outputFolder=$(echo $buildType | awk '{print tolower($0)}')
    
    echo "OutputFolder:${outputFolder}"
    echo "Listing the output dir.. ${CWD}/${appname}/app/build/outputs/apk/${outputFolder}"

    ls -al ${CWD}/${appname}/app/build/outputs/apk/${outputFolder}

    echo Copying the build output to ${CWD}
    if [ "$appname" == "Supplier" ]; then
        supplier_name=$(echo $supplier_name | sed -e 's/[[:space:]]/_/g')
        echo "Updated supplier name is:$supplier_name"
        cp ${CWD}/${appname}/app/build/outputs/apk/${outputFolder}/app-${outputFolder}.apk "${CWD}/${supplier_name}_${appname}_${outputFolder}_${versionName}_${BUILD_TIME}.apk"
    else
        cp ${CWD}/${appname}/app/build/outputs/apk/${outputFolder}/app-${outputFolder}.apk "${CWD}/${appname}_${outputFolder}_${versionName}_${BUILD_TIME}.apk"
    fi
    cd  ${CWD}
    echo "Listing the files from Base folder:${BASE_FOLDER}"
    ls -alh
    echo "Done."
}


    

if [[ "${whattobuild}" == "all" ]]; then
    build_app "Supplier"
    build_app "Customer"
    build_app "Admin_SC"
else
    build_app "${whattobuild}"
fi

#copy_archives
#clean_up


echo "All Done..."