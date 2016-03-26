
import UIKit
import WebKit
import Firebase

public typealias RosefireCallback = ((NSError!, String!) -> ())!

@objc
public class Rosefire : NSObject {
    
    private static var rosefire : Rosefire?
    
    public class func sharedDelegate() -> Rosefire! {
        if rosefire != nil {
            rosefire = Rosefire()
        }
        return rosefire!
    }
    
    public var uiDelegate : UIViewController?
    
    private override init() {
        super.init()
    }
    
    public func signIn(registryToken : String!, withClosure closure: RosefireCallback) {
        if uiDelegate == nil {
            let err = NSError(domain: "Failed to set UI Delegate for Rosefire", code: 500, userInfo: nil)
            closure(err, nil)
            return
        }
        let webview = WebviewController()
        webview.registryToken = registryToken
        webview.callback = { (err, token) in
            self.uiDelegate!.dismissViewControllerAnimated(true, completion: nil)
            closure(err, token)
        }
        uiDelegate!.presentViewController(webview, animated: true, completion: nil)
    }
    
}


@objc
private class WebviewController : UIViewController, WKScriptMessageHandler {
    
    var webview: WKWebView?
    var registryToken: String?
    var callback: RosefireCallback?
    
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
        
        let token = registryToken!.stringByAddingPercentEncodingWithAllowedCharacters(.URLHostAllowedCharacterSet())
        let url = NSURL(string: "https://rosefire.csse.rose-hulman.edu/webview/login?platform=ios&registryToken=\(token)")
        let req = NSURLRequest(URL: url!)
        webview!.loadRequest(req)
    }
    
    @objc func userContentController(userContentController: WKUserContentController, didReceiveScriptMessage message: WKScriptMessage) {
        if(message.name == "rosefire") {
            print("JavaScript is sending a message \(message.body)")
        }
    }
}
