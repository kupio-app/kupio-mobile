import {
    byTestTag,
    byText,
    clearSignedInUser,
    firstDisplayedText,
    hideKeyboardIfOpen,
    openApp,
    scrollToTestTag,
    scrollToText,
    waitForFirstDisplayedText,
} from "../support/app";
import { e2eUser, getE2eOwnerListing } from "../support/backend";

const updatedTitle = "Appium E2E Updated Listing";
const updatedPrice = "658";
const updatedPriceFormatted = "\u20ac658";

async function replaceText(tag: string, value: string) {
    let lastError: unknown;

    for (let attempt = 0; attempt < 3; attempt += 1) {
        const element = await byTestTag(tag);
        await element.waitForDisplayed({ timeout: 15000 });

        try {
            await browser.execute("mobile: replaceElementValue", {
                elementId: element.elementId,
                text: value,
            });
            await browser.waitUntil(async () => (await byTestTag(tag).getText()) === value, {
                timeout: 5000,
                timeoutMsg: `Expected ${tag} to contain ${value}`,
            });
            return;
        } catch (error) {
            lastError = error;
            await browser.pause(500);
        }
    }

    throw lastError;
}

describe("Edit listing e2e", () => {
    it("updates the reusable owner's listing through the Android UI", async () => {
        const listing = await getE2eOwnerListing();

        await clearSignedInUser();
        await openApp();

        await byTestTag("auth.screen").waitForDisplayed({ timeout: 15000 });
        await byTestTag("auth.email").setValue(e2eUser.email);
        await byTestTag("auth.password").setValue(e2eUser.password);
        await hideKeyboardIfOpen();
        await byTestTag("auth.submit").click();

        await byTestTag("main.tabs").waitForDisplayed({ timeout: 20000 });
        await byTestTag("main.me").click();

        await byTestTag("profile.screen").waitForDisplayed({ timeout: 15000 });
        await scrollToTestTag("profile.manage-listings");
        await byTestTag("profile.manage-listings").click();

        await firstDisplayedText(["All", "V\u0161etky"])
            .then((element) => element.click())
            .catch(() => undefined);

        await scrollToText(listing.title);
        await byText(listing.title).click();

        const editButton = await waitForFirstDisplayedText(["Edit", "Upravi\u0165"], 15000);
        await editButton.click();

        await replaceText("listing.create.title", updatedTitle);
        await hideKeyboardIfOpen();
        await scrollToTestTag("listing.create.price");
        await replaceText("listing.create.price", updatedPrice);
        await hideKeyboardIfOpen();

        const saveButton = await firstDisplayedText(["Save changes", "Ulo\u017ei\u0165 zmeny"]);
        await saveButton.click();

        await browser.waitUntil(
            async () => !(await firstDisplayedText(["Save changes", "Ulo\u017ei\u0165 zmeny"]).then(() => true).catch(() => false)),
            {
                timeout: 20000,
                timeoutMsg: "Expected the edit form to close after saving changes",
            }
        );

        await browser.back();
        await scrollToText(updatedTitle);
        await byText(updatedTitle).click();

        await scrollToText("METRICS").catch(() => scrollToText("METRIKY"));
        await scrollToText(updatedPriceFormatted);

        await expect(byText(updatedPriceFormatted)).toBeDisplayed();
        await expect(byText(updatedTitle)).toBeDisplayed();
    });
});
