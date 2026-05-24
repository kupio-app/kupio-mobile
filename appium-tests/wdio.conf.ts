import { resolve } from "node:path";

const appPath = resolve(__dirname, "../composeApp/build/outputs/apk/debug/composeApp-debug.apk");

export const config = {
    runner: "local",
    hostname: "127.0.0.1",
    port: 4723,
    path: "/",

    specs: ["./tests/ui/**/*.spec.ts"],

    maxInstances: 1,

    framework: "mocha",

    reporters: ["spec"],

    mochaOpts: {
        timeout: 120000,
    },

    services: [
        [
            "appium",
            {
                args: {
                    relaxedSecurity: true,
                },
            },
        ],
    ],

    capabilities: [
        {
            maxInstances: 1,
            platformName: "Android",
            "appium:automationName": "UiAutomator2",
            "appium:deviceName": "Android Emulator",
            "appium:app": appPath,
            "appium:appPackage": "kupio.mobile",
            "appium:appActivity": ".MainActivity",
            "appium:appWaitActivity": ".MainActivity",
            "appium:autoGrantPermissions": true,
            "appium:autoLaunch": false,
            "appium:noReset": false,
        },
    ],
};
