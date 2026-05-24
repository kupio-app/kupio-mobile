import { byTestTag, openApp, scrollToTestTag, seedSignedInUser, tapByTestTag } from "../support/app";

describe("Profile", () => {
    it("shows the signed-in user's account state", async () => {
        await seedSignedInUser();
        await openApp();

        await byTestTag("main.tabs").waitForDisplayed({ timeout: 15000 });
        await tapByTestTag("main.me");

        await byTestTag("profile.screen").waitForDisplayed({ timeout: 15000 });
        await byTestTag("profile.username").waitForDisplayed({ timeout: 15000 });

        await expect(byTestTag("profile.user-info")).toBeDisplayed();
        await expect(byTestTag("profile.username")).toHaveText("@appium");

        const displayName = await byTestTag("profile.display-name").getText();
        expect(displayName).toContain("Appium User");

        await expect(byTestTag("profile.balance-card")).toBeDisplayed();
        await expect(byTestTag("profile.balance")).toHaveText("0.00 €");
        await expect(byTestTag("profile.top-up")).toBeEnabled();

        await expect(await scrollToTestTag("profile.settings")).toBeDisplayed();
        await expect(byTestTag("profile.settings")).toBeEnabled();
        await byTestTag("profile.settings").click();

        await byTestTag("settings.screen").waitForDisplayed({ timeout: 15000 });
        await expect(byTestTag("settings.user-id")).toHaveText(expect.stringContaining("appium-user"));
        await expect(byTestTag("settings.logout")).toBeDisplayed();
        await expect(byTestTag("settings.logout")).toBeEnabled();
    });
});
