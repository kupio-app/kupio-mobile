const byTestTag = (tag: string) => $(`android=new UiSelector().resourceId("${tag}")`);

async function hideKeyboardIfOpen() {
    try {
        await browser.hideKeyboard();
    } catch {
        // Keyboard may already be closed on some devices.
    }
}

describe("Auth validation", () => {
    it("validates login fields and reveals register-only fields", async () => {
        await browser.activateApp("kupio.mobile");
        await byTestTag("auth.screen").waitForDisplayed({ timeout: 15000 });

        await byTestTag("auth.submit").click();

        await expect(byTestTag("auth.email-error")).toBeDisplayed();
        await expect(byTestTag("auth.password-error")).toBeDisplayed();

        await byTestTag("auth.switch-mode").click();

        await expect(byTestTag("auth.confirm-password")).toBeDisplayed();
        await expect(byTestTag("auth.username")).toBeDisplayed();

        await byTestTag("auth.email").setValue("student@example.com");
        await byTestTag("auth.password").setValue("password123");
        await byTestTag("auth.confirm-password").setValue("different123");
        await byTestTag("auth.username").setValue("tester");
        await hideKeyboardIfOpen();

        await byTestTag("auth.submit").click();

        await expect(byTestTag("auth.confirm-password-error")).toBeDisplayed();
    });
});
