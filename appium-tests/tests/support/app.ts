import { execFile } from "node:child_process";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);

export const appId = "kupio.mobile";

export const byTestTag = (tag: string) => $(`android=new UiSelector().resourceId("${tag}")`);

const uiSelectorText = (text: string) => text.replace(/\\/g, "\\\\").replace(/"/g, '\\"');

export const byText = (text: string) => $(`android=new UiSelector().text("${uiSelectorText(text)}")`);

export async function firstDisplayedText(texts: string[]) {
    for (const text of texts) {
        const element = byText(text);
        if (await element.isDisplayed().catch(() => false)) {
            return element;
        }
    }
    throw new Error(`None of the expected texts are displayed: ${texts.join(", ")}`);
}

export async function waitForFirstDisplayedText(texts: string[], timeout = 15000) {
    let displayedText: string | null = null;

    await browser.waitUntil(
        async () => {
            for (const text of texts) {
                const element = byText(text);
                if (await element.isDisplayed().catch(() => false)) {
                    displayedText = text;
                    return true;
                }
            }
            return false;
        },
        {
            timeout,
            timeoutMsg: `None of the expected texts are displayed: ${texts.join(", ")}`,
        }
    );

    return byText(displayedText!);
}

export async function scrollToText(text: string) {
    await $(
        `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text("${uiSelectorText(text)}"))`
    );
    return byText(text);
}

export async function tapByTestTag(tag: string) {
    const element = await byTestTag(tag);
    await element.waitForDisplayed({ timeout: 15000 });
    await element.click();
}

export async function scrollToTestTag(tag: string) {
    await $(
        `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().resourceId("${tag}"))`
    );
    return byTestTag(tag);
}

export async function openApp() {
    try {
        await browser.terminateApp(appId);
    } catch {
        // The app may not be running yet in a fresh Appium session.
    }
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
