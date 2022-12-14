/*
 *  Extension to leshan-server-demo for application code.
 */

package org.course;


import java.util.*;

import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.registration.Deregistration;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.server.registration.RegistrationServiceImpl;

public class RoomControl {

    //
    // Static reference to the server.
    //
    private static LeshanServer lwServer;

    //
    // 2IMN15:  TODO  : fill in
    //
    // Declare variables to keep track of the state of the room.
    //
    /**
     * Global variable to keep track of the maximum possible peak power in the room
     * Add all the peak powers of each individual luminaire at registration
     * And remove it at deregistration
     */
    static int maxRoomPeakPower;
    /**
     * Global variable to keep track of each luminare (by regID) and its associated
     * peak power
     */
    static Map<String, Integer> powerMap;
    /**
     * Global array list to keep track of all the registrations of luminaires
     */
    static ArrayList<Registration> registrationLuminaire;
    /**
     * Global variable to keep track of the dim level of each of the luminaires
     */
    static float RoomDimLevel = 0;
    /**
     * Global boolean variable to keep track of the status of the presence detector
     */
    static boolean presence;
    /**
     * Global variable to keep track of the given power budget from DemandResponse
     */
    static int powerBudget;
    /**
     * Global array list to keep track of the allowed types a luminaire can be
     * Will be used to check if a luminaire is of a given type
     * If a luminaire type is not present here, it will not be registered
     */
    static ArrayList<String> allowedType;

    /**
     * Function used for initialization of all required variables
     *
     * @param server the Leshan Server variable
     */
    public static void Initialize(LeshanServer server) {
        // Register the LWM2M server object for future use
        lwServer = server;

        // 2IMN15:  TODO  : fill in
        //
        // Initialize the state variables.
        powerMap = new HashMap<String, Integer>();
        registrationLuminaire = new ArrayList<>();
        // Initialize max room peak power to 0 (corresponding to 0 luminaires
        // being registered)
        maxRoomPeakPower = 0;
        allowedType = new ArrayList<>();
        // Allowed types - can be extended
        allowedType.add("LED");
        allowedType.add("Halogen");

    }

    //
    // Suggested support methods:
    //
    // * set the dim level of all luminaires.
    // * set the power flag of all luminaires.
    // * show the status of the room.

    /**
     * handleDimLevel() function
     * <p>
     * It is used to disseminate for each registered luminare the global dim level
     */
    public static void handleDimLevel() {
        for (Registration registration : registrationLuminaire) {
            writeInteger(registration,
                    Constants.LUMINAIRE_ID,
                    0,
                    Constants.RES_DIM_LEVEL,
                    (int) RoomDimLevel);
        }
    }

    /**
     * handlePower() function
     * <p>
     * It is used to disseminate for each registered luminare the global power status
     * It is adjusted based on the presence global variable
     * That variable can be only true or false, depending on what is being received from
     * the presence detector
     */
    public static void handlePower() {
        for (Registration registration : registrationLuminaire) {
            writeBoolean(registration,
                    Constants.LUMINAIRE_ID,
                    0,
                    Constants.RES_POWER,
                    presence);
        }
    }

    /**
     * handleType() function
     * <p>
     * Function used to disseminate back to the GUI the received parameter for type
     * Function does not take into account the allowed types
     * It will show exactly the type that is given in the CLI command
     * <p>
     * NOTE: This is not required and could have been implemented in the GUI directly
     * This allows for further functionality of possibly changing the type of the luminaire
     * from the GUI of the RoomControl itself
     * How useful that would be is up to discussion
     *
     * @param registration of type Registration
     * @param type         of type String
     */
    public static void handleType(Registration registration, String type) {
        System.out.println(type);
        writeString(registration,
                Constants.LUMINAIRE_ID,
                0,
                Constants.RES_TYPE,
                type);
    }

    /**
     * handlePeakPower() function
     * <p>
     * Function used to disseminate back to the GUI the received parameter for peakPower
     * Function does not take into account the allowed types
     * It will show exactly the type that is given in the CLI command
     * <p>
     * NOTE: This is not required and could have been implemented in the GUI directly
     * This allows for further functionality of possibly changing the peak power of the luminaire
     * from the GUI of the RoomControl itself
     * How useful that would be is up to discussion
     *
     * @param registration of type Registration
     * @param peakPower    of type int
     */
    public static void handlePeakPower(Registration registration, int peakPower) {
        writeInteger(registration,
                Constants.LUMINAIRE_ID,
                0,
                Constants.RES_PEAK_POWER,
                peakPower);
    }

    /**
     * handleAllowedTypes() function
     * <p>
     * Function used to manage deregistration for types that are not allowed
     *
     * @param registration of type Registration
     * @param type         of type String
     */
    public static void handleAllowedTypes(Registration registration, String type) {
        if (!allowedType.contains(type)) {
            // Code according to https://github.com/eclipse/leshan/issues/901
            // How to Deregister a client from Server #901
            RegistrationServiceImpl registrationService = ((RegistrationServiceImpl) lwServer.getRegistrationService());
            final Deregistration deregistration = registrationService.getStore().removeRegistration(registration.getId());
            registrationService.fireUnregistered(deregistration.getRegistration(), deregistration.getObservations(), null);
        }
    }

    /**
     * luminaireRegistrationMonitor() function
     * <p>
     * Used to monitor the state variables for registration and deregistration of luminaires
     * <p>
     * When a luminaire is registered it is added to the array list keeping track of all
     * luminaire registrations. Its peak power is added to the max room peak power and
     * the hash map is completed with the key,value pair of regID,peakPower
     * <p>
     * When a luminaire is deregistered it is removed from the array list keeping track of all
     * luminaire registrations. The hash map is queried for the respective peakPower of the
     * current regID. This peakPower is removed from the total max room peak power. The hash map
     * is updated by removing the respective key,value pair of regID,peakPower
     *
     * @param registration of type Registration
     * @param peakPower    of type Integer
     * @param method       of type String
     */
    public static void luminaireRegistrationMonitor(Registration registration, Integer peakPower, String method) {
        if (Objects.equals(method, "ADD LUMINAIRE")) {
            registrationLuminaire.add(registration);
            maxRoomPeakPower += peakPower;
            powerMap.put(registration.getId(), peakPower);
        } else if (Objects.equals(method, "REMOVE LUMINAIRE")) {
            maxRoomPeakPower -= powerMap.get(registration.getId());
            powerMap.remove(registration.getId());
            registrationLuminaire.remove(registration);
        }

    }

    /**
     * calculateDimLevel() function
     * <p>
     * Function used to calculate the global dim level of all the luminaires
     *
     * @param newPowerBudget of type int
     */
    public static void calculateDimLevel(int newPowerBudget) {
        // if the power budget is larger than 500, keep it at 500
        if (newPowerBudget > 500) {
            newPowerBudget = 500;
        }
        // if the power budget is larger or equal than 0, calculate the dim level
        // if the power budget is smaller than 0, then the power budget
        // has not been updated
        if (newPowerBudget >= 0) {
            powerBudget = newPowerBudget;
            RoomDimLevel = ((float) newPowerBudget / (float) maxRoomPeakPower) * 100;
            // If the dim level is larger than 100, set it at 100 (the max possible)
            if (RoomDimLevel > 100) {
                RoomDimLevel = 100;
            }
        }
    }

    /**
     * calculatePresence() function
     * <p>
     * Used to set the global presence state based on the queried presence state
     * from the presence detector
     *
     * @param newPresenceState of type int
     */
    public static void calculatePresence(int newPresenceState) {
        if (newPresenceState == 1) {
            presence = Boolean.TRUE;
        } else if (newPresenceState == 0) {
            presence = Boolean.FALSE;
        }
    }

    /**
     * Function to handle Registration of all components
     *
     * @param registration of type Registration
     */
    public static void handleRegistration(Registration registration) {
        // Check which objects are available.
        Map<Integer, org.eclipse.leshan.core.LwM2m.Version> supportedObject =
                registration.getSupportedObject();

        if (supportedObject.get(Constants.PRESENCE_DETECTOR_ID) != null) {
            System.out.println("Presence Detector " + registration.getEndpoint());

            //
            // 2IMN15:  TODO  :  fill in
            //
            // Set the initial global presence state
            presence = registerPresenceDetector(registration);
        }

        if (supportedObject.get(Constants.LUMINAIRE_ID) != null) {
            // Get the type of the luminaire
            String type = registerTypeLuminaire(registration);
            // check if the type is allowed
            handleAllowedTypes(registration, type);
            // disseminate the type back to the GUI
            handleType(registration, type);

            System.out.println("Luminaire " + registration.getEndpoint());

            //
            // 2IMN15:  TODO  :  fill in
            //
            // Process the registration of a new Luminaire.

            // Get the peak power of the luminaire
            int peakPower = registerPeakPowerLuminaire(registration);
            // Disseminate the peak power back to the GUI
            handlePeakPower(registration, peakPower);

            // Handle the registration of the luminaire
            luminaireRegistrationMonitor(registration, peakPower, "ADD LUMINAIRE");
            // Calculate the dim level
            calculateDimLevel(powerBudget);
            // Disseminate to the components the global dim level and power state
            handleDimLevel();
            handlePower();
        }

        if (supportedObject.get(Constants.DEMAND_RESPONSE_ID) != null) {
            System.out.println("Demand Response " + registration.getEndpoint());
            //
            // The registerDemandResponse() method contains example code
            // on how handle a registration.
            //
            // Get the initial power budget from demand response
            powerBudget = registerDemandResponse(registration);
            // Calculate the dim level based on this power budget
            calculateDimLevel(powerBudget);
        }

        //  2IMN15: don't forget to update the other luminaires.
    }

    /**
     * handleDeregistration() function
     * <p>
     * Handles the deregistration of all possible components
     *
     * @param registration of type Registration
     */
    public static void handleDeregistration(Registration registration) {
        //
        // 2IMN15:  TODO  :  fill in
        //
        // The device identified by the given registration will
        // disappear.  Update the state accordingly.
        Map<Integer, org.eclipse.leshan.core.LwM2m.Version> supportedObject =
                registration.getSupportedObject();

        if (supportedObject.get(Constants.PRESENCE_DETECTOR_ID) != null) {
            System.out.println("Presence Detector Left " + registration.getEndpoint());
        }

        if (supportedObject.get(Constants.LUMINAIRE_ID) != null) {
            // Handle the deregistration of a luminaire, along with updating all global variables
            luminaireRegistrationMonitor(registration, null, "REMOVE LUMINAIRE");
            // Recalculate the dim level based on the current power budget
            calculateDimLevel(powerBudget);
            // Handle the dim level and power state upon deregistration of a luminaire
            // Used to update both in real time after removal.
            handleDimLevel();
            handlePower();
            System.out.println("Luminaire Left " + registration.getEndpoint());
        }

        if (supportedObject.get(Constants.DEMAND_RESPONSE_ID) != null) {
            System.out.println("Demand Response Left " + registration.getEndpoint());
        }

    }

    /**
     * Observe the response of each of the components
     *
     * @param observation  of type SingleObservation
     * @param registration of type Registration
     * @param response     of type ObserveResponse
     * @throws InterruptedException for a RuntimeException caused in EventServlet
     */
    public static void handleObserveResponse(SingleObservation observation,
                                             Registration registration,
                                             ObserveResponse response) throws InterruptedException {
        if (registration != null && observation != null && response != null) {
            //
            // 2IMN15:  TODO  :  fill in
            //
            // When the registration and observation are known,
            // process the value contained in the response.
            //
            // Useful methods:
            //    registration.getEndpoint()
            //    observation.getPath()

            // For processing an update of the Demand Response object.
            // It contains some example code.
            int newPowerBudget = observedDemandResponse(observation, response);

            // Calculate dim level based on the new power budget
            calculateDimLevel(newPowerBudget);
            // Handle the dim level based on the new power budget
            handleDimLevel();

            // Get the new presence state from the presence detector
            int newPresenceState = observedPresenceDetector(observation, response);
            // Calculate the global presence variable based on the new observation
            calculatePresence(newPresenceState);
            // Handle the global power of luminaires based on the new presence state
            handlePower();
        }
    }


    // Support functions for reading and writing resources of
    // certain types.

    /**
     * Returns the current power budget.
     *
     * @param registration of type Registration
     * @return power budget
     */
    private static int registerDemandResponse(Registration registration) {
        int powerBudget = readInteger(registration,
                Constants.DEMAND_RESPONSE_ID,
                0,
                Constants.RES_TOTAL_BUDGET);
        System.out.println("Power budget is " + powerBudget);
        // Observe the total budget information for updates.
        try {
            ObserveRequest obRequest =
                    new ObserveRequest(Constants.DEMAND_RESPONSE_ID,
                            0,
                            Constants.RES_TOTAL_BUDGET);
            System.out.println(">>ObserveRequest created << ");
            ObserveResponse coResponse =
                    lwServer.send(registration, obRequest, 1000);
            System.out.println(">>ObserveRequest sent << ");
            if (coResponse == null) {
                System.out.println(">>ObserveRequest null << ");
            }
        } catch (Exception e) {
            System.out.println("Observe request failed for Demand Response.");
        }
        return powerBudget;
    }

    /**
     * Function that returns the type of luminaire (what is given in the CLI)
     *
     * @param registration of type Registration
     * @return type of luminare as string
     */
    private static String registerTypeLuminaire(Registration registration) {
        String type = readString(registration,
                Constants.LUMINAIRE_ID,
                0,
                Constants.RES_TYPE);
        System.out.println("Type of luminaire is " + type);
        // Observe the type of luminaire for initialization
        try {
            ObserveRequest obRequest =
                    new ObserveRequest(Constants.LUMINAIRE_ID,
                            0,
                            Constants.RES_TYPE);
            System.out.println(">>ObserveRequest created << ");
            ObserveResponse coResponse =
                    lwServer.send(registration, obRequest, 1000);
            System.out.println(">>ObserveRequest sent << ");
            if (coResponse == null) {
                System.out.println(">>ObserveRequest null << ");
            }
        } catch (Exception e) {
            System.out.println("Observe request failed for Luminaire Type Response.");
        }
        return type;
    }

    /**
     * Function that returns the peak power of luminaire (what is given in the CLI)
     *
     * @param registration of type Registration
     * @return peak power as int
     */
    private static int registerPeakPowerLuminaire(Registration registration) {
        int peakPower = readInteger(registration,
                Constants.LUMINAIRE_ID,
                0,
                Constants.RES_PEAK_POWER);
        System.out.println("Peak power of luminaire is " + peakPower);
        // Observe the total peak power of luminaire for registration;
        try {
            ObserveRequest obRequest =
                    new ObserveRequest(Constants.LUMINAIRE_ID,
                            0,
                            Constants.RES_PEAK_POWER);
            System.out.println(">>ObserveRequest created << ");
            ObserveResponse coResponse =
                    lwServer.send(registration, obRequest, 1000);
            System.out.println(">>ObserveRequest sent << ");
            if (coResponse == null) {
                System.out.println(">>ObserveRequest null << ");
            }
        } catch (Exception e) {
            System.out.println("Observe request failed for Luminaire Peak Power Response.");
        }
        return peakPower;
    }

    /**
     * Function that returns the presence status of the presence detector
     *
     * @param registration of type Registration
     * @return presence as boolean
     */
    private static boolean registerPresenceDetector(Registration registration) {
        boolean presence = readBoolean(registration,
                Constants.PRESENCE_DETECTOR_ID,
                0,
                Constants.RES_PRESENCE);
        System.out.println("Presence Detector is currently in state" + presence);
        // Observe the presence information of presence detector for updates.
        try {
            ObserveRequest obRequest =
                    new ObserveRequest(Constants.PRESENCE_DETECTOR_ID,
                            0,
                            Constants.RES_PRESENCE);
            System.out.println(">>ObserveRequest created << ");
            ObserveResponse coResponse =
                    lwServer.send(registration, obRequest, 1000);
            System.out.println(">>ObserveRequest sent << ");
            if (coResponse == null) {
                System.out.println(">>ObserveRequest null << ");
            }
        } catch (Exception e) {
            System.out.println("Observe request failed for Presence Response.");
        }
        return presence;
    }

    // If the response contains a new power budget, it returns that value.
    // Otherwise, it returns -1.

    /**
     * If the response contains a new power budget, it returns that value.
     * Otherwise, it returns -1.
     *
     * @param observation of type SingleObservation
     * @param response    of type ObserveResponse
     * @return integer
     */
    private static int observedDemandResponse(SingleObservation observation,
                                              ObserveResponse response) {
        // Alternative code:
        // String obsRes = observation.getPath().toString();
        // if (obsRes.equals("/33002/0/30005"))
        LwM2mPath obsPath = observation.getPath();
        if ((obsPath.getObjectId() == Constants.DEMAND_RESPONSE_ID) &&
                (obsPath.getResourceId() == Constants.RES_TOTAL_BUDGET)) {
            String strValue = ((LwM2mResource) response.getContent()).getValue().toString();
            try {
                int newPowerBudget = Integer.parseInt(strValue);
                System.out.println(newPowerBudget);
                return newPowerBudget;
            } catch (Exception e) {
                System.out.println("Exception in reading demand response:" + e.getMessage());
            }
        }
        return -1;
    }

    /**
     * If the response contains a new presence state, it returns that boolean (0,1)
     * Otherwise, it returns -1.
     *
     * @param observation of type SingleObservation
     * @param response    of type ObserveResponse
     * @return integer \in {-1,0,1}
     */
    private static int observedPresenceDetector(SingleObservation observation,
                                                ObserveResponse response) {
        LwM2mPath obsPath = observation.getPath();
        if ((obsPath.getObjectId() == Constants.PRESENCE_DETECTOR_ID) &&
                (obsPath.getResourceId() == Constants.RES_PRESENCE)) {
            String strValue = ((LwM2mResource) response.getContent()).getValue().toString();
            try {
                boolean newPresenceState = Boolean.parseBoolean(strValue);
                if (newPresenceState) {
                    return 1;
                } else {
                    return 0;
                }
            } catch (Exception e) {
                System.out.println("Exception in reading presence detection response:" + e.getMessage());
            }
        }
        return -1;
    }


    private static int readInteger(Registration registration, int objectId, int instanceId, int resourceId) {
        try {
            ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
            ReadResponse cResponse = lwServer.send(registration, request, 5000);
            if (cResponse.isSuccess()) {
                String sValue = ((LwM2mResource) cResponse.getContent()).getValue().toString();
                try {
                    int iValue = Integer.parseInt(((LwM2mResource) cResponse.getContent()).getValue().toString());
                    return iValue;
                } catch (Exception e) {
                }
                float fValue = Float.parseFloat(((LwM2mResource) cResponse.getContent()).getValue().toString());
                return (int) fValue;
            } else {
                return 0;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("readInteger: exception");
            return 0;
        }
    }

    private static String readString(Registration registration, int objectId, int instanceId, int resourceId) {
        try {
            ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
            ReadResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                String value = ((LwM2mResource) cResponse.getContent()).getValue().toString();
                return value;
            } else {
                return "";
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("readString: exception");
            return "";
        }
    }

    /**
     * New function based on readString/readInteger to read a boolean from a resource
     *
     * @param registration of type Registration
     * @param objectId     of type int
     * @param instanceId   of type int
     * @param resourceId   of type int
     * @return boolean
     */
    private static Boolean readBoolean(Registration registration, int objectId, int instanceId, int resourceId) {
        try {
            ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
            ReadResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                Boolean value = Boolean.valueOf(((LwM2mResource) cResponse.getContent()).getValue().toString());
                return value;
            } else {
                return Boolean.FALSE;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("readString: exception");
            return Boolean.FALSE;
        }
    }

    private static void writeInteger(Registration registration, int objectId, int instanceId, int resourceId, int value) {
        try {
            WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
            WriteResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                System.out.println("writeInteger: Success");
            } else {
                System.out.println("writeInteger: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeInteger: exception");
        }
    }

    private static void writeString(Registration registration, int objectId, int instanceId, int resourceId, String value) {
        try {
            WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
            WriteResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                System.out.println("writeString: Success");
            } else {
                System.out.println("writeString: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeString: exception");
        }
    }

    private static void writeBoolean(Registration registration, int objectId, int instanceId, int resourceId, boolean value) {
        try {
            WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
            WriteResponse cResponse = lwServer.send(registration, request, 1000);
            if (cResponse.isSuccess()) {
                System.out.println("writeBoolean: Success");
            } else {
                System.out.println("writeBoolean: Failed, " + cResponse.toString());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("writeBoolean: exception");
        }
    }

}
