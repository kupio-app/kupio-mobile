import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

export const appId = "kupio.mobile";

export const byTestTag = (tag: string) => $(`android=new UiSelector().resourceId("${tag}")`);

export async function scrollToTestTag(tag: string) {
    await $(
        `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId("${tag}"))`
    );
    return byTestTag(tag);
}

export async function openApp() {
    await browser.activateApp(appId);
}

export async function hideKeyboardIfOpen() {
    try {
        await browser.hideKeyboard();
    } catch {
        // Keyboard may already be closed on some devices.
    }
}

export async function seedSignedInUser() {
    await execFileAsync("adb", [
        "shell",
        "am",
        "broadcast",
        "-n",
        `${appId}/.debug.TestAuthReceiver`,
        "-a",
        "kupio.mobile.test.SEED_AUTH",
        "--es",
        "userId",
        "appium-user",
        "--es",
        "email",
        "appium@example.com",
        "--es",
        "username",
        "appium",
    ]);
}

export async function clearSignedInUser() {
    await execFileAsync("adb", [
        "shell",
        "am",
        "broadcast",
        "-n",
        `${appId}/.debug.TestAuthReceiver`,
        "-a",
        "kupio.mobile.test.CLEAR_AUTH",
    ]);
}
