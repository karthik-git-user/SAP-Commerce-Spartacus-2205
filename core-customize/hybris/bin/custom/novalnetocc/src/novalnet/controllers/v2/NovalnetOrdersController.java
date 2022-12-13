package novalnet.controllers.v2;

import novalnet.controllers.NoCheckoutCartException;
import novalnet.controllers.NovalnetPaymentException;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commercewebservicescommons.dto.order.PaymentDetailsWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.PaymentAuthorizationException;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.store.BaseStoreModel;
import javax.annotation.Resource;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import static de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper.DEFAULT_LEVEL;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.SearchResult;
import java.net.URL;

import org.json.JSONObject;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;

import de.hybris.novalnet.core.model.NovalnetPaymentInfoModel;

import de.hybris.novalnet.core.model.NovalnetDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGuaranteedDirectDebitSepaPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGuaranteedInvoicePaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPayPalPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetCreditCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetInvoicePaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPrepaymentPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetBarzahlenPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetInstantBankTransferPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetOnlineBankTransferPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetBancontactPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetMultibancoPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetIdealPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetEpsPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetGiropayPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPrzelewy24PaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPostFinanceCardPaymentModeModel;
import de.hybris.novalnet.core.model.NovalnetPostFinancePaymentModeModel;

import de.hybris.novalnet.core.model.NovalnetCallbackInfoModel;
import de.hybris.platform.core.model.order.payment.PaymentModeModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.order.PaymentModeService;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.commercefacades.user.data.CountryData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.jalo.JaloSession;
import de.novalnet.order.NovalnetOrderFacade;
import de.novalnet.beans.NnResponseData;
import novalnet.dto.payment.NnResponseWsDTO;
import de.novalnet.beans.NnPaymentDetailsData;
import novalnet.dto.payment.NnPaymentDetailsWsDTO;
import novalnet.dto.payment.NnRequestWsDTO;
import de.novalnet.beans.NnCreditCardData;
import de.novalnet.beans.NnDirectDebitSepaData;
import de.novalnet.beans.NnPayPalData;
import de.novalnet.beans.NnRequestData;
import de.novalnet.beans.NnBillingData;
import de.novalnet.beans.NnCountryData;
import de.novalnet.beans.NnRegionData;
import de.novalnet.beans.NnPaymentData;
import de.novalnet.beans.NnPaymentsData;
import de.novalnet.beans.NnConfigData;
import novalnet.dto.payment.NnConfigWsDTO;
import java.text.NumberFormat;
import java.text.DecimalFormat;

// import com.mirakl.hybris.fulfilmentprocess.actions.order.CreateMarketplaceOrderAction;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/novalnet/orders")
@ApiVersion("v2")
@Tag(name = "Novalnet Orders")
public class NovalnetOrdersController 
{
    private final static Logger LOG = Logger.getLogger(NovalnetOrdersController.class);
    
    protected static final String DEFAULT_FIELD_SET = FieldSetLevelHelper.DEFAULT_LEVEL;
    
    private BaseStoreModel baseStore;
    private CartData cartData;
    private CartModel cartModel;
    private String password;
    public String message = "";
   
    private static final String PAYMENT_AUTHORIZE = "AUTHORIZE";

    @Resource(name = "novalnetOrderFacade")
    NovalnetOrderFacade novalnetOrderFacade;
    
    @Resource(name = "dataMapper")
    private DataMapper dataMapper;
    
    @Resource
    private PaymentModeService paymentModeService;

    private static final String REQUEST_MAPPING = "paymentType,action,cartId,billingAddress(titleCode,firstName,lastName,line1,line2,town,postalCode,country(isocode),region(isocode)),paymentData(panHash,uniqId,iban),returnUrl,tid";
    
    protected static final String API_COMPATIBILITY_B2C_CHANNELS = "api.compatibility.b2c.channels";
    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
    @PostMapping(value = "/placeOrder", consumes = { MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE })
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
    @Operation(operationId = "placeOrder", summary = "Place a order.", description = "Authorizes the cart and places the order. The response contains the new order data.")
    @ApiBaseSiteIdAndUserIdParam
    public OrderWsDTO placeOrder(
    @Parameter(description =
    "Request body parameter that contains details such as the name on the card (accountHolderName), the card number (cardNumber), the card type (cardType.code), "
            + "the month of the expiry date (expiryMonth), the year of the expiry date (expiryYear), whether the payment details should be saved (saved), whether the payment details "
            + "should be set as default (defaultPaymentInfo), and the billing address (billingAddress.firstName, billingAddress.lastName, billingAddress.titleCode, billingAddress.country.isocode, "
            + "billingAddress.line1, billingAddress.line2, billingAddress.town, billingAddress.postalCode, billingAddress.region.isocode)\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final NnRequestWsDTO orderRequest,
    @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
    throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException, NovalnetPaymentException
    {
        NnRequestData requestData = dataMapper.map(orderRequest, NnRequestData.class, fields);
        requestData.getAction();
        cartData = novalnetOrderFacade.loadCart(requestData.getCartId());
        cartModel = novalnetOrderFacade.getCart();
        baseStore = novalnetOrderFacade.getBaseStoreModel();

        String action               = requestData.getAction();
        final String emailAddress   = JaloSession.getCurrentSession().getUser().getLogin();
        String responseString       = "";

        final UserModel currentUser = novalnetOrderFacade.getCurrentUserForCheckout();
        String totalAmount          = formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
        DecimalFormat decimalFormat = new DecimalFormat("##.##");
        String orderAmount          = decimalFormat.format(Float.parseFloat(totalAmount));
        float floatAmount           = Float.parseFloat(orderAmount);
        BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
        orderAmountCents = orderAmountCents.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        Integer orderAmountCent     = orderAmountCents.intValue();
        final String currency       = cartData.getTotalPriceWithTax().getCurrencyIso();
        final Locale language       = JaloSession.getCurrentSession().getSessionContext().getLocale();
        final String languageCode   = language.toString().toUpperCase();

        password = baseStore.getNovalnetPaymentAccessKey().trim();
            
        if("create_order".equals(action)) {
            Map<String, Object> requsetDeatils = new HashMap<String, Object>();
            try {
                requsetDeatils = formPaymentRequest(requestData, action, emailAddress, orderAmountCent, currency, languageCode);
            } catch(Exception ex) {
                LOG.error("Novalnet Exception " + message);
                throw new NovalnetPaymentException(message);
            }
            StringBuilder response = sendRequest(requsetDeatils.get("paygateURL").toString(), requsetDeatils.get("jsonString").toString());
            responseString = response.toString();
        } else {
            responseString = getTransactionDetails(requestData, languageCode);
        }

        JSONObject tomJsonObject    = new JSONObject(responseString);
        JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
            final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
            LOG.info("Error message recieved from novalnet for cart id: " + requestData.getCartId() + " " + statMessage);
            throw new PaymentAuthorizationException();
        }

        JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
        String[] successStatus = {"CONFIRMED", "ON_HOLD", "PENDING"};

        if (Arrays.asList(successStatus).contains(transactionJsonObject.get("status").toString())) {
        
            JSONObject customerJsonObject = tomJsonObject.getJSONObject("customer");
            JSONObject billingJsonObject = customerJsonObject.getJSONObject("billing");

            AddressModel billingAddress =  new AddressModel();

            NnBillingData billingData =  requestData.getBillingAddress();

            if(billingData != null && billingData.getAddressId() != null ) {
                billingAddress = novalnetOrderFacade.getBillingAddress(billingData.getAddressId());
            } else {
                billingAddress = novalnetOrderFacade.getModelService().create(AddressModel.class);
                
                billingAddress.setFirstname(customerJsonObject.get("first_name").toString());
                billingAddress.setLastname(customerJsonObject.get("last_name").toString());
                if (billingJsonObject.has("street")) {
                    billingAddress.setLine1(billingJsonObject.get("street").toString());
                }
                billingAddress.setLine2("");
                billingAddress.setTown(billingJsonObject.get("city").toString());
                billingAddress.setPostalcode(billingJsonObject.get("zip").toString());
                billingAddress.setCountry(novalnetOrderFacade.getCommonI18NService().getCountry(billingJsonObject.get("country_code").toString()));
                billingAddress.setEmail(emailAddress);
            }

            billingAddress.setOwner(cartModel);

            String currentPayment = transactionJsonObject.get("payment_type").toString();

            String payment = currentPayment.equals("CREDITCARD") ? "novalnetCreditCard" :(currentPayment.equals("DIRECT_DEBIT_SEPA") ? "novalnetDirectDebitSepa" :(currentPayment.equals("PAYPAL") ? "novalnetPayPal": (currentPayment.equals("GUARANTEED_DIRECT_DEBIT_SEPA") ? "novalnetGuaranteedDirectDebitSepa" : (currentPayment.equals("INVOICE") ? "novalnetInvoice":(currentPayment.equals("GUARANTEED_INVOICE") ? "novalnetGuaranteedInvoice":(currentPayment.equals("PREPAYMENT") ? "novalnetPrepayment":(currentPayment.equals("MULTIBANCO") ? "novalnetMultibanco":(currentPayment.equals("CASHPAYMENT") ? "novalnetBarzahlen":(currentPayment.equals("ONLINE_BANK_TRANSFER") ? "novalnetOnlineBankTransfer":((currentPayment.equals("ONLINE_TRANSFER") ? "novalnetInstantBankTransfer":(currentPayment.equals("BANCONTACT") ? "novalnetBancontact":(currentPayment.equals("POSTFINANCE_CARD") ? "novalnetPostFinanceCard":(currentPayment.equals("POSTFINANCE") ? "novalnetPostFinance":(currentPayment.equals("IDEAL") ? "novalnetIdeal":(currentPayment.equals("EPS") ? "novalnetEps":(currentPayment.equals("GIROPAY") ? "novalnetGiropay":(currentPayment.equals("PRZELEWY24") ? "novalnetPrzelewy24":""))))))))))))))))));

            OrderData orderData = createOrder(transactionJsonObject, payment, billingAddress, emailAddress, currentUser, orderAmountCent, currency, languageCode);
            return dataMapper.map(orderData, OrderWsDTO.class, fields);
        } else {
            final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
            LOG.info("Error message recieved from novalnet for cart id: " + requestData.getCartId().toString() + " " + statMessage);
            throw new PaymentAuthorizationException();
        }
    }
    
    public List<OrderModel> getOrderInfoModel(String orderCode) {
        // Initialize StringBuilder
        StringBuilder query = new StringBuilder();

        // Select query for fetch OrderModel
        query.append("SELECT {pk} from {" + OrderModel._TYPECODE + "} where {" + OrderModel.CODE
                + "} = ?code");
        FlexibleSearchQuery executeQuery = new FlexibleSearchQuery(query.toString());

        // Add query parameter
        executeQuery.addQueryParameter("code", orderCode);

        // Execute query
        SearchResult<OrderModel> result = novalnetOrderFacade.getFlexibleSearchService().search(executeQuery);
        return result.getResult();
    }

    public Map<String, Object> formPaymentRequest(NnRequestData requestData, String action, String emailAddress, Integer orderAmountCent, String currency, String languageCode) throws NovalnetPaymentException {

        final Map<String, Object> transactionParameters = new HashMap<String, Object>();
        final Map<String, Object> merchantParameters    = new HashMap<String, Object>();
        final Map<String, Object> customerParameters    = new HashMap<String, Object>();
        final Map<String, Object> billingParameters     = new HashMap<String, Object>();
        final Map<String, Object> shippingParameters    = new HashMap<String, Object>();
        final Map<String, Object> customParameters      = new HashMap<String, Object>();
        final Map<String, Object> paymentParameters     = new HashMap<String, Object>();
        final Map<String, Object> dataParameters        = new HashMap<String, Object>();
        final Map<String, Object> responeParameters     = new HashMap<String, Object>();

        final AddressModel deliveryAddress  = cartModel.getDeliveryAddress();

        boolean verify_payment_data = false;

        Integer testMode            = 0;
        Integer onholdOrderAmount   = 0;
        Integer sameAsBilling       = 0;
        String customerNo           = JaloSession.getCurrentSession().getUser().getPK().toString();
        String currentPayment       = requestData.getPaymentType();

        String payment = currentPayment.equals("CREDITCARD") ? "novalnetCreditCard" :(currentPayment.equals("DIRECT_DEBIT_SEPA") ? "novalnetDirectDebitSepa" :(currentPayment.equals("PAYPAL") ? "novalnetPayPal": (currentPayment.equals("GUARANTEED_DIRECT_DEBIT_SEPA") ? "novalnetGuaranteedDirectDebitSepa" : (currentPayment.equals("INVOICE") ? "novalnetInvoice":(currentPayment.equals("GUARANTEED_INVOICE") ? "novalnetGuaranteedInvoice":(currentPayment.equals("PREPAYMENT") ? "novalnetPrepayment":(currentPayment.equals("MULTIBANCO") ? "novalnetMultibanco":(currentPayment.equals("CASHPAYMENT") ? "novalnetBarzahlen":(currentPayment.equals("ONLINE_BANK_TRANSFER") ? "novalnetOnlineBankTransfer":((currentPayment.equals("ONLINE_TRANSFER") ? "novalnetInstantBankTransfer":(currentPayment.equals("BANCONTACT") ? "novalnetBancontact":(currentPayment.equals("POSTFINANCE_CARD") ? "novalnetPostFinanceCard":(currentPayment.equals("POSTFINANCE") ? "novalnetPostFinance":(currentPayment.equals("IDEAL") ? "novalnetIdeal":(currentPayment.equals("EPS") ? "novalnetEps":(currentPayment.equals("GIROPAY") ? "novalnetGiropay":(currentPayment.equals("PRZELEWY24") ? "novalnetPrzelewy24":""))))))))))))))))));

        String[] supportedPaymentTypes = {"novalnetCreditCard", "novalnetDirectDebitSepa", "novalnetGuaranteedDirectDebitSepa", "novalnetInvoice", "novalnetGuaranteedInvoice", "novalnetPrepayment", "novalnetMultibanco", "novalnetBarzahlen", "novalnetPayPal", "novalnetInstantBankTransfer", "novalnetOnlineBankTransfer", "novalnetBancontact", "novalnetPostFinanceCard", "novalnetPostFinance", "novalnetIdeal", "novalnetEps", "novalnetGiropay", "novalnetPrzelewy24"};

        if("".equals(payment)) {
            message = "Payment method is not valid";
            throw new NovalnetPaymentException(message);
        }

        if (!Arrays.asList(supportedPaymentTypes).contains(payment)) {
            message = "Currently the payment method is not supported";
            throw new NovalnetPaymentException(message);
        }

        PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(payment);
        NnBillingData billingData =  requestData.getBillingAddress();
        String firstName, lastName, street1, street2, town, zip, countryCode;
        firstName = lastName = street1 = street2 = town = zip = countryCode = "";

        if(billingData.getAddressId() != null ) {

            AddressModel billingAddress = novalnetOrderFacade.getBillingAddress(billingData.getAddressId());

            firstName    = billingAddress.getFirstname();
            lastName     = billingAddress.getLastname();
            street1      = billingAddress.getLine1();
            street2      = (billingAddress.getLine2() != null) ? billingAddress.getLine2() : "";
            town         = billingAddress.getTown();
            zip          = billingAddress.getPostalcode();
            countryCode  = billingAddress.getCountry().getIsocode();
        } else {

            NnCountryData countryData =  billingData.getCountry();
            NnRegionData regionData   =  billingData.getRegion();

            firstName    = billingData.getFirstName();
            lastName     = billingData.getLastName();
            street1      = billingData.getLine1();
            street2      = (billingData.getLine2() != null) ? billingData.getLine2() : "";
            town         = billingData.getTown();
            zip          = billingData.getPostalCode();
            countryCode  = countryData.getIsocode();

        }

        Gson gson = new GsonBuilder().create();

        merchantParameters.put("signature", baseStore.getNovalnetAPIKey());
        merchantParameters.put("tariff", baseStore.getNovalnetTariffId());

        customerParameters.put("first_name", firstName);
        customerParameters.put("last_name", lastName);
        customerParameters.put("email", emailAddress);
        customerParameters.put("customer_no", customerNo);
        customerParameters.put("gender", "u");

        billingParameters.put("street", street1 + " " + street2);
        billingParameters.put("city", town);
        billingParameters.put("zip", zip);
        billingParameters.put("country_code", countryCode);

        if( deliveryAddress.getLine1().toString().toLowerCase().equals(street1.toLowerCase()) && ((deliveryAddress.getLine2() == null && street2.equals("")) || (deliveryAddress.getLine2() != null && (deliveryAddress.getLine2().toString().toLowerCase().equals(street2.toLowerCase())))) && deliveryAddress.getTown().toString().toLowerCase().equals(town.toLowerCase()) &&  deliveryAddress.getPostalcode().toString().equals(zip) && deliveryAddress.getCountry().getIsocode().toString().equals(countryCode)) {
            sameAsBilling = 1;
            shippingParameters.put("same_as_billing", 1);
        } else {
            shippingParameters.put("street", deliveryAddress.getLine1() + " " + deliveryAddress.getLine2());
            shippingParameters.put("city", deliveryAddress.getTown());
            shippingParameters.put("zip", deliveryAddress.getPostalcode());
            shippingParameters.put("country_code", deliveryAddress.getCountry().getIsocode());
            shippingParameters.put("first_name", deliveryAddress.getFirstname());
            shippingParameters.put("last_name", deliveryAddress.getLastname());
        }
        
        customParameters.put("lang", languageCode);

        transactionParameters.put("payment_type", currentPayment);
        transactionParameters.put("currency", currency);
        transactionParameters.put("amount", orderAmountCent);
        transactionParameters.put("system_name", "SAP Commerce Cloud");
        transactionParameters.put("system_version", "2105-NN1.0.1");

        Map<String, String> responseDeatils = novalnetOrderFacade.getBackendConfiguration("payment", payment);

        if (responseDeatils.get("active").equals("false")) {
            message = "Payment method is not active";
            throw new NovalnetPaymentException(message);
        }

        if (responseDeatils.get("test_mode").toString() == "true") {
            testMode = 1;
        }

        transactionParameters.put("test_mode", testMode);

        if ("novalnetGuaranteedDirectDebitSepa".equals(payment) || "novalnetGuaranteedInvoice".equals(payment)) {

            
            boolean isValidDob = false;

            if(billingData.getDob() != null) {
                isValidDob = novalnetOrderFacade.hasAgeRequirement(billingData.getDob());
            }

            String[] guaranteeRequiredCountry = {"CH", "AT", "DE"};
            Integer minimumAmount = (responseDeatils.get("guarantee_minimum_amount") == null) ? 0 : Integer.parseInt(responseDeatils.get("guarantee_minimum_amount").toString());

            if(sameAsBilling == 1 && (isValidDob || billingData.getCompany() != null) && Arrays.asList(guaranteeRequiredCountry).contains(countryCode) && orderAmountCent >= minimumAmount) {

                if(billingData.getCompany() != null) {
                    billingParameters.put("company", billingData.getCompany());
                }

                if(billingData.getDob() != null) {
                    customerParameters.put("birth_date", billingData.getDob());
                } 
                
            } else if (responseDeatils.get("force_guarantee").equals("true")) {
                transactionParameters.put("payment_type", (currentPayment.equals("GUARANTEED_INVOICE") ? "INVOICE" : "DIRECT_DEBIT_SEPA"));
                payment = payment.equals("novalnetGuaranteedInvoice") ? "novalnetInvoice" : "novalnetDirectDebitSepa";
                responseDeatils = novalnetOrderFacade.getBackendConfiguration("payment", payment);
            } else {
                message = "Gaurantee payment conditions are not met";
                throw new NovalnetPaymentException(message);
            }
        }

        String[] onholdSupportedPaymentTypes = {"novalnetCreditCard", "novalnetDirectDebitSepa", "novalnetGuaranteedDirectDebitSepa", "novalnetInvoice", "novalnetGuaranteedInvoice",  "novalnetPayPal"};

         if (Arrays.asList(onholdSupportedPaymentTypes).contains(payment)) {
            onholdOrderAmount = (responseDeatils.get("onhold_amount") == null) ? 0 : Integer.parseInt(responseDeatils.get("onhold_amount").toString());

            if (PAYMENT_AUTHORIZE.equals(responseDeatils.get("onhold_action").toString()) && orderAmountCent >= onholdOrderAmount) {
                 verify_payment_data = true;
            }

        }

        String[] dueDatePaymentTypes = {"novalnetBarzahlen", "novalnetDirectDebitSepa", "novalnetGuaranteedDirectDebitSepa", "novalnetInvoice", "novalnetPrepayment"};

        if (Arrays.asList(dueDatePaymentTypes).contains(payment)) {
            Integer dueDate = Integer.parseInt(responseDeatils.get("due_date").toString());

            Integer sendDueDate = 0;

            if (payment.equals("novalnetInvoice") && dueDate != null && dueDate > 7) {
                sendDueDate = 1;
            } else if (payment.equals("novalnetPrepayment") && dueDate != null && dueDate > 7 && dueDate < 28) {
                sendDueDate = 1;
            } else if(payment.equals("novalnetBarzahlen") && dueDate != null ) {
                sendDueDate = 1;
            }  else if((payment.equals("novalnetDirectDebitSepa") || payment.equals("novalnetGuaranteedDirectDebitSepa"))  && dueDate != null  && dueDate > 2 && dueDate < 14) {
                sendDueDate = 1;
            }

            if(sendDueDate == 1) {
                transactionParameters.put("due_date", formatDate(dueDate));
            }
        }
        
        if ("novalnetCreditCard".equals(payment)) {

            NnPaymentsData paymentData  =  requestData.getPaymentData();

            if(paymentData.getPanHash() != null && paymentData.getUniqId() != null) {
                paymentParameters.put("pan_hash", paymentData.getPanHash());
                paymentParameters.put("unique_id", paymentData.getUniqId());
            } else {
                message = "Panhash and UniqId is reuired to process payment" + payment;
                LOG.info(message);
                throw new NovalnetPaymentException(message);
            }

            if (responseDeatils.get("enforce_3d").toString() == "true") {
                 transactionParameters.put("enforce_3d", 1);
            }

        } else if ("novalnetDirectDebitSepa".equals(payment) || "novalnetGuaranteedDirectDebitSepa".equals(payment)) {
            NnPaymentsData paymentData  =  requestData.getPaymentData();

            String accountHolder = billingData.getFirstName() + ' ' + billingData.getLastName();

            if(paymentData.getIban() != null) {
                paymentParameters.put("iban", paymentData.getIban());
            } else {
                message = "IBAN is required to process payment" + payment;
                LOG.info(message);
                throw new NovalnetPaymentException(message);
            }

            String ibanCountry = paymentData.getIban().substring(0,2);
            String[] bicRequiredCountry = {"CH", "MC", "SM", "GB"};

            if (Arrays.asList(bicRequiredCountry).contains(ibanCountry)) {
                if(paymentData.getBic() != null) {
                    paymentParameters.put("bic", paymentData.getBic());
                } else {
                    message = "BIC is required to process payment";
                    throw new NovalnetPaymentException(message);
                }
            }

            paymentParameters.put("bank_account_holder", accountHolder.replace("&", ""));

        }

        if(action.equals("get_redirect_url")) {
            if("".equals(requestData.getReturnUrl())) {
                message = "returnUrl is empty. Please send the mandatorey params";
                throw new NovalnetPaymentException(message);
            }

            transactionParameters.put("return_url", requestData.getReturnUrl());
            transactionParameters.put("error_return_url", requestData.getReturnUrl());
        }

        customerParameters.put("billing", billingParameters);
        customerParameters.put("shipping", shippingParameters);

        transactionParameters.put("payment_data", paymentParameters);
        dataParameters.put("merchant", merchantParameters);
        dataParameters.put("customer", customerParameters);
        dataParameters.put("transaction", transactionParameters);
        dataParameters.put("custom", customParameters);
        
        String jsonString = gson.toJson(dataParameters);

        String url = "https://payport.novalnet.de/v2/payment";
        if(verify_payment_data == true) {
            url = "https://payport.novalnet.de/v2/authorize";
        }

        responeParameters.put("jsonString", jsonString);
        responeParameters.put("paygateURL", url);

        return responeParameters;
    }

    public static String formatDate(int date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendarInsatance = Calendar.getInstance();
        calendarInsatance.add(calendarInsatance.DATE, date);
        return dateFormat.format(calendarInsatance.getTime());
    }


    public String getTransactionDetails(NnRequestData requestData, String languageCode) {

        Gson gson = new GsonBuilder().create();

        final Map<String, Object> transactionParameters = new HashMap<String, Object>();
        final Map<String, Object> customParameters      = new HashMap<String, Object>();
        final Map<String, Object> dataParameters        = new HashMap<String, Object>();

        transactionParameters.put("tid", requestData.getTid());
        customParameters.put("lang", languageCode);
        dataParameters.put("transaction", transactionParameters);
        dataParameters.put("custom", customParameters);

        String jsonString = gson.toJson(dataParameters);
        String url = "https://payport.novalnet.de/v2/transaction/details";
        StringBuilder response = sendRequest(url, jsonString);
        return response.toString();
    }


    public OrderData createOrder(JSONObject transactionJsonObject, String payment, AddressModel billingAddress, String emailAddress, UserModel currentUser, Integer orderAmountCent, String currency, String languageCode)
    throws InvalidCartException, NoCheckoutCartException {

        PaymentModeModel paymentModeModel = paymentModeService.getPaymentModeForCode(payment);
        
        NovalnetPaymentInfoModel paymentInfoModel = new NovalnetPaymentInfoModel();
        paymentInfoModel.setBillingAddress(billingAddress);
        paymentInfoModel.setPaymentEmailAddress(emailAddress);
        paymentInfoModel.setDuplicate(Boolean.FALSE);
        paymentInfoModel.setSaved(Boolean.TRUE);
        paymentInfoModel.setUser(currentUser);
        paymentInfoModel.setPaymentInfo(transactionJsonObject.get("tid").toString());
        paymentInfoModel.setOrderHistoryNotes("Novalnet Transaction ID : "+ transactionJsonObject.get("tid").toString());
        paymentInfoModel.setPaymentProvider(payment);
        paymentInfoModel.setPaymentGatewayStatus(transactionJsonObject.get("status").toString());
        cartModel.setPaymentInfo(paymentInfoModel);
        paymentInfoModel.setCode("");
        
        PaymentTransactionEntryModel orderTransactionEntry = null;
        final List<PaymentTransactionEntryModel> paymentTransactionEntries = new ArrayList<>();
        orderTransactionEntry = novalnetOrderFacade.createTransactionEntry(transactionJsonObject.get("tid").toString(),
                                            cartModel, orderAmountCent, "Novalnet Transaction ID : " + transactionJsonObject.get("tid").toString(), currency);
        paymentTransactionEntries.add(orderTransactionEntry);

        // Initiate/ Update PaymentTransactionModel
        PaymentTransactionModel paymentTransactionModel = new PaymentTransactionModel();
        paymentTransactionModel.setPaymentProvider(payment);
        paymentTransactionModel.setRequestId(transactionJsonObject.get("tid").toString());
        paymentTransactionModel.setEntries(paymentTransactionEntries);
        paymentTransactionModel.setOrder(cartModel);
        paymentTransactionModel.setInfo(paymentInfoModel);

        cartModel.setPaymentTransactions(Arrays.asList(paymentTransactionModel));
        novalnetOrderFacade.getModelService().saveAll(cartModel);

        final OrderData orderData = novalnetOrderFacade.getCheckoutFacade().placeOrder();
        String orderNumber = orderData.getCode();
        List<OrderModel> orderInfoModel = getOrderInfoModel(orderNumber);
        OrderModel orderModel = novalnetOrderFacade.getModelService().get(orderInfoModel.get(0).getPk());

        paymentInfoModel.setCode(orderNumber);
        novalnetOrderFacade.getModelService().save(paymentInfoModel);

        if ("novalnetCreditCard".equals(payment)) {
            NovalnetCreditCardPaymentModeModel novalnetPaymentMethod = (NovalnetCreditCardPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetDirectDebitSepa".equals(payment)) {

            NovalnetDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetDirectDebitSepaPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetGuaranteedInvoice".equals(payment)) {
            NovalnetGuaranteedInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedInvoicePaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetGuaranteedDirectDebitSepa".equals(payment)) {
            NovalnetGuaranteedDirectDebitSepaPaymentModeModel novalnetPaymentMethod = (NovalnetGuaranteedDirectDebitSepaPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetPayPal".equals(payment)) {
            NovalnetPayPalPaymentModeModel novalnetPaymentMethod = (NovalnetPayPalPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetInvoice".equals(payment)) {
            NovalnetInvoicePaymentModeModel novalnetPaymentMethod = (NovalnetInvoicePaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetPrepayment".equals(payment)) {
            NovalnetPrepaymentPaymentModeModel novalnetPaymentMethod = (NovalnetPrepaymentPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);

        } else if ("novalnetBarzahlen".equals(payment)) {
            NovalnetBarzahlenPaymentModeModel novalnetPaymentMethod = (NovalnetBarzahlenPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetInstantBankTransfer".equals(payment)) {
            NovalnetInstantBankTransferPaymentModeModel novalnetPaymentMethod = (NovalnetInstantBankTransferPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetOnlineBankTransfer".equals(payment)) {
            NovalnetOnlineBankTransferPaymentModeModel novalnetPaymentMethod = (NovalnetOnlineBankTransferPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetMultibanco".equals(payment)) {
            NovalnetMultibancoPaymentModeModel novalnetPaymentMethod = (NovalnetMultibancoPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetBancontact".equals(payment)) {
            NovalnetBancontactPaymentModeModel novalnetPaymentMethod = (NovalnetBancontactPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetPostFinanceCard".equals(payment)) {
            NovalnetPostFinanceCardPaymentModeModel novalnetPaymentMethod = (NovalnetPostFinanceCardPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetPostFinance".equals(payment)) {
            NovalnetPostFinancePaymentModeModel novalnetPaymentMethod = (NovalnetPostFinancePaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetIdeal".equals(payment)) {
            NovalnetIdealPaymentModeModel novalnetPaymentMethod = (NovalnetIdealPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetEps".equals(payment)) {
            NovalnetEpsPaymentModeModel novalnetPaymentMethod = (NovalnetEpsPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetGiropay".equals(payment)) {
            NovalnetGiropayPaymentModeModel novalnetPaymentMethod = (NovalnetGiropayPaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        } else if ("novalnetPrzelewy24".equals(payment)) {
            NovalnetPrzelewy24PaymentModeModel novalnetPaymentMethod = (NovalnetPrzelewy24PaymentModeModel) paymentModeModel;
            orderModel.setPaymentMode(novalnetPaymentMethod);
            // cartModel.setPaymentMode(novalnetPaymentMethod);
        }

        orderModel.setStatusInfo("Novalnet Transaction ID : " + transactionJsonObject.get("tid").toString());
        
        OrderHistoryEntryModel orderEntry = novalnetOrderFacade.getModelService().create(OrderHistoryEntryModel.class);
        orderEntry.setTimestamp(new Date());
        orderEntry.setOrder(orderModel);
        orderEntry.setDescription("Novalnet Transaction ID : " + transactionJsonObject.get("tid").toString());
        orderModel.setPaymentInfo(paymentInfoModel);
        novalnetOrderFacade.getModelService().saveAll(orderModel, orderEntry);
        novalnetOrderFacade.updateOrderStatus(orderNumber, paymentInfoModel);
        createTransactionUpdate(transactionJsonObject.get("tid").toString(), orderNumber, languageCode);
        
        long callbackInfoTid = Long.parseLong(transactionJsonObject.get("tid").toString());
        int orderPaidAmount = 0;
        String[] bankPayments = {"novalnetInvoice", "novalnetPrepayment", "novalnetBarzahlen", "novalnetGuaranteedDirectDebitSepa", "novalnetGuaranteedInvoice"};
        boolean isInvoicePrepayment = Arrays.asList(bankPayments).contains(payment);

        String[] pendingStatusCode = {"PENDING"};

        // Check for payment pending payments
        if (isInvoicePrepayment || Arrays.asList(pendingStatusCode).contains(transactionJsonObject.get("status").toString())) {
            orderPaidAmount = 0;
        } else {
            orderPaidAmount = orderAmountCent;
        }

        NovalnetCallbackInfoModel novalnetCallbackInfo = new NovalnetCallbackInfoModel();
        novalnetCallbackInfo.setPaymentType(payment);
        novalnetCallbackInfo.setOrderAmount(orderAmountCent);
        novalnetCallbackInfo.setCallbackTid(callbackInfoTid);
        novalnetCallbackInfo.setOrginalTid(callbackInfoTid);
        novalnetCallbackInfo.setPaidAmount(orderPaidAmount);
        novalnetCallbackInfo.setOrderNo(orderNumber);
        novalnetOrderFacade.getModelService().save(novalnetCallbackInfo);

        return orderData;
    }

    public void createTransactionUpdate(String tid, String orderNumber, String languageCode) {

        Gson gson = new GsonBuilder().create();

        final Map<String, Object> transactionParameters = new HashMap<String, Object>();
        final Map<String, Object> customParameters      = new HashMap<String, Object>();
        final Map<String, Object> dataParameters        = new HashMap<String, Object>();

        transactionParameters.put("tid", tid);
        transactionParameters.put("order_no", orderNumber);
        customParameters.put("lang", languageCode);
        dataParameters.put("transaction", transactionParameters);
        dataParameters.put("custom", customParameters);
        String jsonString = gson.toJson(dataParameters);
        String url = "https://payport.novalnet.de/v2/transaction/update";
        StringBuilder response = sendRequest(url, jsonString);
    }
    
    public StringBuilder sendRequest(String url, String jsonString) {
        StringBuilder response = new StringBuilder();
        try {
            LOG.info("request sent to novalnet");
            LOG.info(jsonString);
            String urly = url;
            URL obj = new URL(urly);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            byte[] postData = jsonString.getBytes(StandardCharsets.UTF_8);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Charset", "utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("X-NN-Access-Key", Base64.getEncoder().encodeToString(password.getBytes()));

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(postData);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader iny = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String output;


            while ((output = iny.readLine()) != null) {
                response.append(output);
            }
            iny.close();
        } catch (MalformedURLException ex) {
            LOG.error("MalformedURLException ", ex);
        } catch (IOException ex) {
            LOG.error("IOException ", ex);
        }

        LOG.info("response recieved from novalnet");
        LOG.info(response.toString());

        return response;
    }
    
    public static String formatAmount(String amount) {
        if (amount.contains(",")) {
            try {
                NumberFormat formattedAmount = NumberFormat.getNumberInstance(Locale.GERMANY);
                double formattedValue = formattedAmount.parse(amount).doubleValue();
                amount = Double.toString(formattedValue);
            } catch (Exception e) {
                amount = amount.replace(",", ".");
            }
        }
        return amount;
    }
    
    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
    @PostMapping(value = "/getRedirectURL", consumes = { MediaType.APPLICATION_JSON_VALUE,
    MediaType.APPLICATION_XML_VALUE })
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
    @Operation(operationId = "redirecturl", summary = "redirect url", description = "Get the third party url for customer to pay")
    @ApiBaseSiteIdAndUserIdParam
    public NnResponseWsDTO getRedirectURL(
            @Parameter(description =
            "Request body parameter that contains details such as the name on the card (accountHolderName), the card number (cardNumber), the card type (cardType.code), "
                    + "the month of the expiry date (expiryMonth), the year of the expiry date (expiryYear), whether the payment details should be saved (saved), whether the payment details "
                    + "should be set as default (defaultPaymentInfo), and the billing address (billingAddress.firstName, billingAddress.lastName, billingAddress.titleCode, billingAddress.country.isocode, "
                    + "billingAddress.line1, billingAddress.line2, billingAddress.town, billingAddress.postalCode, billingAddress.region.isocode)\n\nThe DTO is in XML or .json format.", required = true) @RequestBody final NnRequestWsDTO orderRequest,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException, NovalnetPaymentException
    {
        NnRequestData requestData = dataMapper.map(orderRequest, NnRequestData.class, fields);
        cartData  = novalnetOrderFacade.loadCart(requestData.getCartId());
        cartModel = novalnetOrderFacade.getCart();
        baseStore = novalnetOrderFacade.getBaseStoreModel();

        String action = "get_redirect_url";
        final String emailAddress   = JaloSession.getCurrentSession().getUser().getLogin();
        String responseString = "";

        final UserModel currentUser = novalnetOrderFacade.getCurrentUserForCheckout();
        String totalAmount          = formatAmount(String.valueOf(cartData.getTotalPriceWithTax().getValue()));
        DecimalFormat decimalFormat = new DecimalFormat("##.##");
        String orderAmount          = decimalFormat.format(Float.parseFloat(totalAmount));
        float floatAmount           = Float.parseFloat(orderAmount);
        BigDecimal orderAmountCents = BigDecimal.valueOf(floatAmount).multiply(BigDecimal.valueOf(100));
        orderAmountCents = orderAmountCents.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        Integer orderAmountCent     = orderAmountCents.intValue();
        final String currency       = cartData.getTotalPriceWithTax().getCurrencyIso();
        final Locale language       = JaloSession.getCurrentSession().getSessionContext().getLocale();
        final String languageCode   = language.toString().toUpperCase();

        password = baseStore.getNovalnetPaymentAccessKey().trim();
        Map<String, Object> requsetDeatils = new HashMap<String, Object>();
        try {
            requsetDeatils = formPaymentRequest(requestData, action, emailAddress, orderAmountCent, currency, languageCode);
        } catch(Exception ex) {
            LOG.error("Novalnet Exception " + message);
            throw new NovalnetPaymentException(message);
        }
        
        StringBuilder response = sendRequest(requsetDeatils.get("paygateURL").toString(), requsetDeatils.get("jsonString").toString());
        JSONObject tomJsonObject = new JSONObject(response.toString());
        JSONObject resultJsonObject = tomJsonObject.getJSONObject("result");
        JSONObject transactionJsonObject = tomJsonObject.getJSONObject("transaction");
        
        if(!String.valueOf("100").equals(resultJsonObject.get("status_code").toString())) {
            final String statMessage = resultJsonObject.get("status_text").toString() != null ? resultJsonObject.get("status_text").toString() : resultJsonObject.get("status_desc").toString();
            throw new PaymentAuthorizationException();
        }

        String redirectURL = resultJsonObject.get("redirect_url").toString();
        NnResponseData responseData = new NnResponseData();
        responseData.setRedirectURL(redirectURL);
        return dataMapper.map(responseData, NnResponseWsDTO.class, fields);
    }

    @Secured({ "ROLE_CUSTOMERGROUP", "ROLE_CLIENT", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT" })
    @RequestMapping(value = "/paymentDetails", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2C_CHANNELS)
    @Operation(operationId = "paymnetdeatils", summary = "Payment Details.", description = "Payment details for the order")
    @ApiBaseSiteIdAndUserIdParam
    public NnPaymentDetailsWsDTO getPaymentDetails(
            @Parameter(description = "order no", required = true) @RequestParam final String orderno,
            @ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
            throws PaymentAuthorizationException, InvalidCartException, NoCheckoutCartException
    {
        final List<NovalnetPaymentInfoModel> paymentInfo = novalnetOrderFacade.getNovalnetPaymentInfo(orderno);
        NovalnetPaymentInfoModel paymentInfoModel = novalnetOrderFacade.getPaymentModel(paymentInfo);
        NnPaymentDetailsData paymentDetailsData = new NnPaymentDetailsData();
        paymentDetailsData.setStatus(paymentInfoModel.getPaymentGatewayStatus());
        paymentDetailsData.setComments(paymentInfoModel.getOrderHistoryNotes());
        paymentDetailsData.setTid(paymentInfoModel.getPaymentInfo().toString());
        return dataMapper.map(paymentDetailsData, NnPaymentDetailsWsDTO.class, fields);
    }
    
    public static String getPaymentType(String paymentName) {
        final Map<String, String> paymentType = new HashMap<String, String>();
        paymentType.put("novalnetCreditCard", "CREDITCARD");
        paymentType.put("novalnetDirectDebitSepa", "DIRECT_DEBIT_SEPA");
        paymentType.put("novalnetGuaranteedDirectDebitSepa", "GUARANTEED_DIRECT_DEBIT_SEPA");
        paymentType.put("novalnetInvoice", "INVOICE");
        paymentType.put("novalnetGuaranteedInvoice", "GUARANTEED_INVOICE");
        paymentType.put("novalnetPrepayment", "PREPAYMENT");
        paymentType.put("novalnetBarzahlen", "CASHPAYMENT");
        paymentType.put("novalnetPayPal", "PAYPAL");
        paymentType.put("novalnetInstantBankTransfer", "ONLINE_TRANSFER");
        paymentType.put("novalnetOnlineBankTransfer", "ONLINE_BANK_TRANSFER");
        paymentType.put("novalnetBancontact", "BANCONTACT");
        paymentType.put("novalnetMultibanco", "MULTIBANCO");
        paymentType.put("novalnetIdeal", "IDEAL");
        paymentType.put("novalnetEps", "EPS");
        paymentType.put("novalnetGiropay", "GIROPAY");
        paymentType.put("novalnetPrzelewy24", "PRZELEWY24");
        paymentType.put("novalnetPostFinanceCard", "POSTFINANCE_CARD");
        paymentType.put("novalnetPostFinance", "POSTFINANCE");
        return paymentType.get(paymentName);
    }

}
