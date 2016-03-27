
import UIKit
import WebKit

public typealias RosefireCallback = ((NSError!, String!) -> ())!

@objc
public class Rosefire : NSObject {
    
    private static var rosefire : Rosefire?
    
    @objc public class func sharedDelegate() -> Rosefire! {
        if rosefire == nil {
            rosefire = Rosefire()
        }
        return rosefire!
    }
    
    public var uiDelegate : UIViewController?
    private var webview : WebviewController!
    
    private override init() {
        super.init()
    }
    
    @objc public func signIn(registryToken : String!, withClosure closure: RosefireCallback) {
        if uiDelegate == nil {
            let err = NSError(domain: "Failed to set UI Delegate for Rosefire", code: 500, userInfo: nil)
            closure(err, nil)
            return
        }
        webview = WebviewController()
        webview.registryToken = registryToken
        webview.callback = { (err, token) in
            self.uiDelegate!.dismissViewControllerAnimated(true, completion: nil)
            closure(err, token)
        }
        // Is this robust?
        let rootCtrl = UINavigationController(rootViewController: webview)
        webview.navigationItem.leftBarButtonItem = UIBarButtonItem(title: "Cancel",
                                                                   style: .Plain,
                                                                   target: self,
                                                                   action: #selector(cancelled))
        uiDelegate!.presentViewController(rootCtrl, animated: true, completion: nil)
    }
    
    @objc private func cancelled() {
        
        let err = NSError(domain: "User cancelled login", code: 0, userInfo: nil)
        self.webview?.callback(err, nil)
    }
    
}

private class WebviewController : UIViewController, WKScriptMessageHandler, WKNavigationDelegate, WKUIDelegate {
    
    var webview: WKWebView?
    var registryToken: String?
    var callback: RosefireCallback
    
    private override func loadView() {
        super.loadView()
        let contentController = WKUserContentController()
        contentController.addScriptMessageHandler(
            self,
            name: "rosefire"
        )
        let config = WKWebViewConfiguration()
        config.userContentController = contentController
        webview = WKWebView(
            frame: view.bounds,
            configuration: config
        )
        view = webview!
    }
    
    private override func viewDidLoad() {
        super.viewDidLoad()
        
        let token = registryToken!.stringByAddingPercentEncodingForRFC3986()!
        let rosefireUrl = "https://rosefire.csse.rose-hulman.edu/webview/login?registryToken=\(token)&platform=ios"
        let url = NSURL(string: rosefireUrl)
        let req = NSURLRequest(URL: url!)
        webview!.loadRequest(req)
    }
    
    @objc func userContentController(userContentController: WKUserContentController, didReceiveScriptMessage message: WKScriptMessage) {
        if(message.name == "rosefire") {
            callback(nil, message.body as! String)
        }
    }
}

// From http://useyourloaf.com/blog/how-to-percent-encode-a-url-string/
extension String {
    func stringByAddingPercentEncodingForRFC3986() -> String? {
        let unreserved = "-._~/?"
        let allowed = NSMutableCharacterSet.alphanumericCharacterSet()
        allowed.addCharactersInString(unreserved)
        return stringByAddingPercentEncodingWithAllowedCharacters(allowed)
    }
}
