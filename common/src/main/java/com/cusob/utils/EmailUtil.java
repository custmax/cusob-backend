package com.cusob.utils;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.io.File;

public class EmailUtil {

    public static void sendEmail(){
//        String privateKey = "-----BEGIN PRIVATE KEY----- MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDKNyQ9hnutd+cX YeMOI6eu9ntHkCa51w9TKx4OOHp8BoOMqE4+kPLNClesHh8MvR4VVIXIguLOb5+5 Ih6Rqd/9X/Z8GAUjWregnlxZG6OKh+ZeD2X7EgV3eELZjO+pHuLwPvv1/S1rCqhs QQomvV54KsiBuLR+dLBnUao8BKeF0IVQwucOHPRdoRjg6+2YnqtGH3Jw8+Rhc8G5 8E7BelFJVj3Zjbat4dg7creGXXXsERh1Lo61fgKlxbw0EE3SfdQXJ/571y2B4zlY mIW2qPPYQ+QmhCNnInNBfsRiUTmF/pE9eWc2WWGdPdn+UV23QSQk/zGcrlJlJftN ljJTta7FAgMBAAECggEAAw4wvb4/tagLZZrMybynrrjaNYNb1nCFsoHDBz96uE+H 4tLzNbZOeOMoXnkpCnH8F6HqkJRLtYdRwEQf9p78u7EcwhTB1R4siqCB8DW+85uo kzDD33aO/xZh2jQrNeC52Jv3BilAa67iSxpzexDe8c1hsLYYAhYkyTbebeA7IMY2 MR4X0pHoa0tbR3ZIPK34NI1SbgkCsUkvUaJZiy/sjieghavWaQBzhapKPI/7BsVy KjA12FidJyx9sJNjztCxj6fA93I8haNeC4pZNL8PE5DcyJwioP771cIh78wpBrfF 3OdQ4KGOoq//2B5hz6xZ4VB4k1mZx5cUTNl8I4AiYQKBgQDmlXC9EpvjWAJ8GmkJ Z5ObV/el6YqK+TX8tCJsqXgX4+NneJwiGfCZ9442xlKnTR+qBB7ZkdfcCFagLZkh nu4TANQQkVtu8S1wT8sYGDS4GNg9gJ+v0sHIt/cQ9FQl/One78HReiweiJUfRiyW X59Fo4sUKMevat368gXGx7loYQKBgQDggTWc6tWrkKHjuLquK7Cspx6w1uwJgyi8 3IbPBLFPKAC4LxXhqJhe35IPbVL526NPYmwvRo9WLEtGZ8cmdgA+Hvi0aHWtZ/UC xd+Giqxg0CzvGwXuy5t1ac4Y6sOqaeqZ9P+TKd79msvhReI6Thb3FkVJYgR2VWGw Gv1TL11Q5QKBgCL3MltZgQ4egHH1R0xHyphZ5JHiMkIUvF0Pq9CyeiWVRxyfq314 VYXjhktEE7CO/2g2q77uYL9a/FfZ0kjI4u06yO2iPS07oVpQn0mOEiCAk/nvkUeG PCCF1Q9bHY+NJKHL1aZAUoRmzcYqTASZh3t161cpP+6X1AWDweB81N7BAoGAYkYt QdNKVR+qvhRPI47cd7dwJ9js2f0gCAxAtr5HH29TYUy7N1yl1mHnfflNKTKL+mQL F2a7t+0LxnYHJz7gPFEybUaMvKuVBPG5Nxvc+UQnZ0NdBXr7QhperWmW3TIgD+S/ O5xIkJsCREpjDULBqseE7OqLKYGWkZA+8/r3zkECgYAUtMTRtNkqzc7EKvGayYDr TtfRpPTJtRpQfCvRWP5tWsJP0vMlGGg3oNys1d436jXG8Nh2IsBipG6wjCSnpQ82 vhi/PFRlTM/31ztu5rYIanbavfHn6DSy6cotfqcOgyTqq08DVm9+WlGwpzhxIFxt AaPJew9MDc2tHjgxp2X4fw== -----END PRIVATE KEY-----";
        // todo
        String key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCMMojU/RSDM+1ZqfyEF1Ii0Hv0xoxN/7jqW96CvXpHKoGEohAhkRlWeYOZBYRVV0YwefH8KLyOYEHzpRbdwn1gybEFjNXhdGs2YKr5vybz55fY2c/lF15bU9YxNOMgkYCTRdF0NfoJLIfn45LDxmD1jYGPGEhTCvGIFmNxqvWddzt7WKgKq/pEwI8FSF73qGPP9Sibz9uobIzoTJUvA16xxFzFnxVLEURLdRJvnOZg2ANVF3jfGclNxReBcOujoYNiBZtDsS5eAoYkwb2CyKVASGlEoNE6YAf5R0pWm7nRabU7l86MaRsdNpqRo5IStH5hkcxbsMVt46ksc8mi5+K9AgMBAAECggEAH7Nxk0+iqsQ8IDE6wxeO4AZqcP/QS/bN9lhNqRx1INBwtu/6W8y2gQI7Coff03hHi8CXQiBn+n2zjAdBc2V9+jvBBnNBDZjIgWQ8ZPqfMTml8UfXJYMyN85uOXQvA3dDLon0AaYO4rIqDPo9fn1GAWA88oFYIUbxNIdzdwlkPnWX/GdrCBZ71eMxf6OJ/wKNk9V/MBISeLm+c3f/C2AepNfNz08KZ/WtzDMHEh1sD9oymXDRWH6vOetATubFdjnrlOzolmckyyywuxct3ODqtHoXSjfcT5nJS+YQ0Fzo37sy8ujDAe/pAPuK8MwrAHeDsgVsCrOvZIRKONR3Ms9TgQKBgQDaDJdLtNzzJxh1oSWQLq6Vl9z8Ku7XZQl/hhr+5kFgiVfKFTzBv5VefIu6getB+i7/sQ30qVUrVYGnp7S/77LDQ0akoH7moqyZFAIvOAqwo231XHqvxni/Wa1xjssu7EaUespigTSRktR9no+UOVB2iHEQk9PcsauFSEuL+CVc3QKBgQCkmS0QQ5vt57BMVqBM748WRFdZLHcKrQDXFY201mTGouWgMjn6AgY9tMb6hPscN2zw2ggwoKp8EMktTdtl2dhLzzPspad0Na/cqGh5Bnl78G8TG+cRPZ+TkX+KsIhUFlpPaU8XLSKODRymsgkPVoFpr2gLS8Nf9vFixUZQ/j3PYQKBgQCtsuAHaBGnBVrfwbp5rCZZdruMierv3UX+t2bj7arg6yfrDJX0xdiIh3CL55jwWwt7lqDGaOo29gut8dP0Hk9qazpoRI4yzh3uZ9kBooQpiFc84Sm+wr1HuXmIf7buLzxeZZ65b7Hf0XbCh0BbVHj7o99l1E0ufr5jE19TJh8lZQKBgHQcVeyuNNYLZZNEXIpJR5/Gj/Oo6UTPq9MOMkuKHcb97IwoQju5p9tpph/dJSLPensg4zwiaoFgefoKfSWJGgrIXPGoHctdf/RhnGkbhyFGB6TsPwErfmFj9xvH0jO3HPf96rwHq+OXHkn9N2TpehsqkHO/e1A77CXn0eWao9GhAoGBAK6gW7ARNgD00Ko6BPHGKoHvZRPH1S+TCLdDl7pv8mkbTTRKT8lrvwEeknKDTD19vJbtZtxIXN3J6VKpWLy1byzAGXNIovTFfG16v1NiMlDDE40IfoELSWercymcr3qTnVN2qWuu156NdQ39IkBuXrmXZgWbxUZ7IiLaLtutXOIB";
        String domain = "daybreakhust.top";
        String selector = "S1";


        Email email = EmailBuilder.startingBlank()
                .from("daybreak", "daybreak@chtrak.com")
                .to("ming", "2218098884@qq.com")
                .withPlainText("Please view this email in a modern email client!")
                .withSubject("Hello ming,nice to meet you!")
//                .signWithDomainKey(key.getBytes(), domain, selector) todo
                .buildEmail();

        Mailer mailer = MailerBuilder
                .withSMTPServerHost("smtp.feishu.cn")
                .withSMTPServerPort(465)
                .withSMTPServerUsername("daybreak@chtrak.com")
                .withSMTPServerPassword("lzTPdotqdzcJ1FT1")
                .withTransportStrategy(TransportStrategy.SMTPS)
                .buildMailer();

        mailer.sendMail(email);
    }

    public static void main(String[] args) {
        sendEmail();
    }
}
