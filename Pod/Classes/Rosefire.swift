



import Alamofire
import Firebase

@objc
public class RosefireTokenOptions : NSObject {
    var admin : Bool!
    var expires : NSNumber!
    var notBefore : NSNumber!
    var group : Bool!
}

@objc
public class Rosefire : NSObject {
    
public class func getToken(registryToken: String!, email: String!, password: String!, withCompletionBlock closure: ((NSError!, String!) -> ())!, withOptions options: RosefireTokenOptions?) {
    var payload : [String:AnyObject] = [
        "email": email,
        "password": password,
        "registryToken": registryToken
    ]
    if let o = options {
        var requestOptions : [String:AnyObject] = [:]
        if o.admin != nil {
            requestOptions["admin"] = o.admin
        }
        if o.expires != nil {
            requestOptions["expires"] = o.expires
        }
        if o.notBefore != nil {
            requestOptions["notBefore"] = o.notBefore
        }
        if o.group != nil {
            requestOptions["group"] = o.group
        }
        payload["options"] = requestOptions
    }
    Alamofire.request(.POST, "https://rosefire.csse.rose-hulman.edu/api/auth", parameters: payload, encoding: .JSON).responseJSON { response in
        if let httpError = response.result.error {
            let statusCode = httpError.code
            let message : String!
            if statusCode == 401 || statusCode == 400 {
                message = "Invalid Rose-Hulman Credentials"
            } else {
                message = "Login Failed!"
            }
            let userInfo: [NSObject : AnyObject] = [
                NSLocalizedDescriptionKey :  NSLocalizedString("Unauthorized", value: message, comment: ""),
                NSLocalizedFailureReasonErrorKey : NSLocalizedString("Unauthorized", value: message, comment: "")
            ]
            let err = NSError(domain: "rose-hulman.edu", code: statusCode, userInfo: userInfo)
            closure(err, nil)
        } else {
            if let json = response.result.value as? NSDictionary {
                if let token = json["token"] as? String {
                    closure(nil, token)
                } else {
                    let message = json["message"] as! String
                    let code = json["code"] as! Int
                    let userInfo: [NSObject : AnyObject] = [
                        NSLocalizedDescriptionKey :  NSLocalizedString("Invalid Login", value: message, comment: ""),
                        NSLocalizedFailureReasonErrorKey : NSLocalizedString("Invalid Login", value: message, comment: "")
                    ]
                    let err = NSError(domain: "rose-hulman.edu", code: code, userInfo: userInfo)
                    closure(err, nil)
                }
            } else {
                let message = "Could not reach server!"
                let userInfo: [NSObject : AnyObject] = [
                    NSLocalizedDescriptionKey :  NSLocalizedString("Connection Error", value: message, comment: ""),
                    NSLocalizedFailureReasonErrorKey : NSLocalizedString("Connection Error", value: message, comment: "")
                ]
                let err = NSError(domain: "rose-hulman.edu", code: 502, userInfo: userInfo)
                closure(err, nil)
            }
        }
    }
}

}

extension Firebase {
    
    public func authWithRoseHulman(registryToken: String!, email: String!, password: String!, withCompletionBlock closure: ((NSError!, FAuthData!) -> ())!) {
        self.authWithRoseHulman(registryToken, email: email, password: password, withCompletionBlock: closure, withOptions: nil)
    }
    
    public func authWithRoseHulman(registryToken: String!, email: String!, password: String!, withCompletionBlock closure: ((NSError!, FAuthData!) -> ())!, withOptions options: RosefireTokenOptions?) {
        Rosefire.getToken(registryToken, email: email, password: password, withCompletionBlock: { (err, token) -> () in
            if err == nil {
                self.authWithCustomToken(token, withCompletionBlock: closure)
            } else {
                closure(err, nil)
            }
            }, withOptions: options)
        }
}