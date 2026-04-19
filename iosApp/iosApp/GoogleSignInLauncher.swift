import ComposeApp
import GoogleSignIn
import UIKit

final class IOSGoogleSignInLauncher: NSObject, KupioMobileGoogleSignInLauncher {
    func signIn(callback: any KupioMobileGoogleSignInCallback) {
        guard !configuredClientId().isEmpty else {
            callback.onFailure(message: "Set KUPIO_GOOGLE_IOS_CLIENT_ID before using Google sign-in.")
            return
        }

        guard let presentingViewController = UIApplication.shared.topViewController() else {
            callback.onFailure(message: "Unable to open the Google sign-in flow.")
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: presentingViewController) { signInResult, error in
            if let error {
                if let signInError = error as? NSError,
                   signInError.domain == kGIDSignInErrorDomain,
                   signInError.code == GIDSignInError.canceled.rawValue {
                    callback.onCancelled()
                    return
                }

                callback.onFailure(message: error.localizedDescription)
                return
            }

            guard let signInResult else {
                callback.onFailure(message: "Google sign-in did not return a result.")
                return
            }

            signInResult.user.refreshTokensIfNeeded { user, refreshError in
                if let refreshError {
                    callback.onFailure(message: refreshError.localizedDescription)
                    return
                }

                guard let tokenString = user?.idToken?.tokenString, !tokenString.isEmpty else {
                    callback.onFailure(message: "Google did not return an ID token.")
                    return
                }

                callback.onSuccess(idToken: tokenString)
            }
        }
    }

    private func configuredClientId() -> String {
        Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String ?? ""
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
