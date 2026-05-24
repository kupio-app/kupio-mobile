const backendBaseUrl = process.env.KUPIO_E2E_BACKEND_URL ?? "http://localhost:9988";

export const e2eUser = {
    email: "appium-tests-e2e@gmail.com",
    password: "12345678",
    deviceId: "appium-e2e-device",
};

export type BackendListing = {
    id: string;
    title: string;
    description: string;
    price: number;
    currency: string;
    status: string;
};

type LoginResponse = {
    access_token: string;
};

type MyListingsResponse = {
    listings: BackendListing[];
};

async function requestJson<T>(path: string, init: RequestInit): Promise<T> {
    const response = await fetch(`${backendBaseUrl}${path}`, {
        ...init,
        headers: {
            "content-type": "application/json",
            ...(init.headers ?? {}),
        },
    });

    const body = await response.text();
    if (!response.ok) {
        throw new Error(`Backend request failed: ${response.status} ${response.statusText} ${path}\n${body}`);
    }

    return JSON.parse(body) as T;
}

export async function getE2eOwnerListing(): Promise<BackendListing> {
    const login = await requestJson<LoginResponse>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({
            email: e2eUser.email,
            password: e2eUser.password,
            device_id: e2eUser.deviceId,
        }),
    });

    const myListings = await requestJson<MyListingsResponse>("/api/users/me/listings?limit=10", {
        method: "GET",
        headers: {
            authorization: `Bearer ${login.access_token}`,
        },
    });

    if (myListings.listings.length !== 1) {
        throw new Error(
            `Expected the Appium e2e user to have exactly one listing, got ${myListings.listings.length}`
        );
    }

    const listing = myListings.listings[0];
    console.log("Appium e2e fixture user:", {
        email: e2eUser.email,
        password: e2eUser.password,
    });
    console.log("Appium e2e fixture listing:", {
        id: listing.id,
        title: listing.title,
        price: listing.price,
        currency: listing.currency,
        status: listing.status,
    });

    return listing;
}
