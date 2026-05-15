import { byTestTag, openApp, scrollToTestTag, seedSignedInUser } from "../support/app";

describe("Create listing", () => {
    it("opens the create listing screen for a signed-in user", async () => {
        await seedSignedInUser();
        await openApp();

        await byTestTag("main.tabs").waitForDisplayed({ timeout: 15000 });
        await byTestTag("main.create").click();

        await byTestTag("listing.create.screen").waitForDisplayed({ timeout: 15000 });
        await expect(byTestTag("listing.create.topbar")).toBeDisplayed();
        await expect(byTestTag("listing.create.back")).toBeDisplayed();
        await expect(byTestTag("listing.form.photos")).toBeDisplayed();
        await expect(byTestTag("listing.form.photos.hero")).toBeDisplayed();
        await expect(byTestTag("listing.form.photos.add")).toBeDisplayed();
        await expect(byTestTag("listing.create.title")).toBeDisplayed();
        await expect(byTestTag("listing.create.description")).toBeDisplayed();
        await expect(byTestTag("listing.create.category")).toBeDisplayed();
        await expect(await scrollToTestTag("listing.create.filters-placeholder")).toBeDisplayed();
        await expect(await scrollToTestTag("listing.create.price")).toBeDisplayed();
        await expect(byTestTag("listing.create.currency")).toBeDisplayed();
        await expect(byTestTag("listing.create.free-toggle")).toBeDisplayed();
        await expect(byTestTag("listing.create.tradable-toggle")).toBeDisplayed();
        await expect(byTestTag("listing.create.save-draft")).toBeDisplayed();
        await expect(byTestTag("listing.create.publish")).toBeDisplayed();
    });
});
