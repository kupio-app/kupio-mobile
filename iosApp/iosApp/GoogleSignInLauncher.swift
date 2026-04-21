import ComposeApp
import GoogleSignIn
import UIKit

final class IOSGoogleSignInLauncher: NSObject, GoogleSignInLauncher {
    func signIn(callback: any GoogleSignInCallback) {
        let clientId = configuredClientId()
        guard !clientId.isEmpty else {
            callback.onFailure(errorCode: "not_configured")
            return
        }
        let serverClientId = configuredServerClientId()

        guard let presentingViewController = UIApplication.shared.topViewController() else {
            callback.onFailure(errorCode: "activity_unavailable")
            return
        }

        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: clientId,
            serverClientID: serverClientId.isEmpty ? nil : serverClientId
        )

        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { signInResult, error in
            if let error {
                if let signInError = error as? NSError,
                   signInError.domain == kGIDSignInErrorDomain,
                   signInError.code == GIDSignInError.canceled.rawValue {
                    callback.onCancelled()
                    return
                }

                NSLog("Google sign-in failed: %@", error.localizedDescription)
                callback.onFailure(errorCode: "failed")
                return
            }

            guard let signInResult else {
                callback.onFailure(errorCode: "invalid_response")
                return
            }

            signInResult.user.refreshTokensIfNeeded { user, refreshError in
                if let refreshError {
                    NSLog("Google token refresh failed: %@", refreshError.localizedDescription)
                    callback.onFailure(errorCode: "failed")
                    return
                }

                guard let tokenString = user?.idToken?.tokenString, !tokenString.isEmpty else {
                    callback.onFailure(errorCode: "missing_id_token")
                    return
                }

                callback.onSuccess(idToken: tokenString)
            }
        }
    }

    private func configuredClientId() -> String {
        Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String ?? ""
    }

    private func configuredServerClientId() -> String {
        Bundle.main.object(forInfoDictionaryKey: "GIDServerClientID") as? String ?? ""
    }
}

private extension UIApplication {
    func topViewController(
        base: UIViewController? = nil
    ) -> UIViewController? {
        let startingController = base ?? connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController

        if let navigationController = startingController as? UINavigationController {
            return topViewController(base: navigationController.visibleViewController)
        }
        if let tabBarController = startingController as? UITabBarController {
            return topViewController(base: tabBarController.selectedViewController)
        }
        if let presentedViewController = startingController?.presentedViewController {
            return topViewController(base: presentedViewController)
        }

        return startingController
    }
}
