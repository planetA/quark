package edu.quark.managedbeans;


import java.math.BigInteger;
import java.security.cert.CertPathValidatorException.Reason;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.primefaces.event.ScheduleEntryMoveEvent;
import org.primefaces.event.ScheduleEntryResizeEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.LazyScheduleModel;
import org.primefaces.model.ScheduleEvent;
import org.primefaces.model.ScheduleModel;

import edu.quark.businesslogic.AppointmentManager;
import edu.quark.businesslogic.GroupManager;
import edu.quark.businesslogic.ResearcherManager;
import edu.quark.dao.AppointmentDAO;
import edu.quark.dao.GroupDAO;
import edu.quark.dao.ResearcherDAO;
import edu.quark.datatypes.AppointmentDetails;
import edu.quark.datatypes.AppointmentType;
import edu.quark.datatypes.GroupDetails;
import edu.quark.datatypes.GroupType;
import edu.quark.datatypes.ResearcherDetails;
import edu.quark.datatypes.TimeInfo;
import edu.quark.model.Appointment;
import edu.quark.model.ConferenceAppointment;
import edu.quark.model.ProjectGroupMeeting;
import edu.quark.model.ResearchGroupMeeting;
import edu.quark.model.Researcher;
import edu.quark.model.TeachingAppointment;
import edu.quark.systemlogic.CreateAppointment;
import edu.quark.systemlogic.DeleteAppointment;

import java.util.ArrayList;
import java.util.List;


@ManagedBean
@ViewScoped
public class ScheduleView {

	public AppointmentType getType() {
		return type;
	}


	public void setType(AppointmentType type) {
		this.type = type;
	}


	@EJB
	private AppointmentDAO appointmentDAO;
	@EJB
	private GroupManager groupManager;
	@EJB
	private ResearcherDAO researcherDAO;
	@EJB
	private CreateAppointment createAppointment;
	@EJB
	private DeleteAppointment deleteAppointment; 
	@EJB
	private ResearcherManager researcherManager;
	@EJB
	private AppointmentManager appointmentManager;
	
	@ManagedProperty(value="#{credentials}")
	private Credentials credentials;

	private ScheduleModel eventModel;
	private ScheduleModel lazyEventModel;
    private Appointment appointment;
    
    private AppointmentType type;
	private List<Researcher> availableParticipants;
	private List<Researcher> selectedParticipants;
	private List<String> selectedParticipantsNames;

	private ScheduleEvent event = new DefaultScheduleEvent();

	@PostConstruct
	public void init() {
		
		lazyEventModel = new LazyScheduleModel() {
			private static final long serialVersionUID = -1508233823680543048L;

			@Override
			public void loadEvents(Date start, Date end) {
				
			}
		};
		eventModel = new DefaultScheduleModel();
		eventModel.clear();
		availableParticipants = new ArrayList<Researcher>();

//        if(credentials.getResearcher()!=null) {
//        	Date t1=new Date();
//            Date t2=new Date();
//            Date t3=new Date();
//            Date t4=new Date();
//            t1.setHours(18);
//            t2.setHours(19);
//            t3.setHours(11);
//            t4.setHours(23);
//            appointmentManager.createAppointment(
//            		credentials.getResearcher().getRid(),
//            		AppointmentType.CONFERENCE_APPOINTMENT, 
//            		null, "3076", "JJJ", new TimeInfo(t1,t2));
//            appointmentManager.createAppointment(
//            		credentials.getResearcher().getRid(),
//            		AppointmentType.GENERIC_APPOINTMENT, 
//            		null, "3076", "XXX", new TimeInfo(t3,t4));
           this.AppointmentDetailsToView();
//        }
        
	}
    

	public Date getInitialDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(calendar.get(Calendar.YEAR), Calendar.FEBRUARY,
				calendar.get(Calendar.DATE), 0, 0, 0);

		return calendar.getTime();
	}

	private void AppointmentDetailsToView() {
		eventModel.clear();
		List<AppointmentDetails> as = researcherManager.getAppointmentDetails(
				credentials.getResearcher().getRid(), 
				new TimeInfo(new Date(1,0,0), new Date(9999,12,12)));
		List<Appointment> aps = appointmentDAO.findAll();
		System.out.println("Length "+as.size());
		for (AppointmentDetails a : as) {
			Appointment at = appointmentDAO.read(a.getaId());
			AppointmentType t = a.getType();
			TimeInfo ti = a.getTimeInterval();
			DefaultScheduleEvent e = new DefaultScheduleEvent(a.getDescription()+" at "+a.getLocation(),
					ti.getStart(),ti.getEnd(), "evtype"+t.ordinal());
			e.setData(at);
			eventModel.addEvent(e);
		}
	}
	
	private Calendar today() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DATE), 0, 0, 0);
		return calendar;
	}

	public Appointment getAppointment() {
		return appointment;
	}

	public void setAppointment(Appointment appointment) {
		this.appointment = appointment;
	}

	public void addEvent(ActionEvent actionEvent) {
		BigInteger aid = createAppointment.createAppointment(credentials.getResearcher()
				.getRid(), type, null, appointment.getLocation(),
				appointment.getDescription(),
				new TimeInfo(appointment.getStart(),
						appointment.getEnd()));
		for (String email : selectedParticipantsNames) {
			Researcher r = researcherManager.checkEmail(email);
			appointmentManager.inviteResearcher(r.getRid(), aid);
		}

		this.AppointmentDetailsToView();
		/*if (appointmentDetails.getId() == null)
			eventModel.addEvent(appointmentDetails);
		else
			eventModel.updateEvent(appointmentDetails);

		appointmentDetails = new DefaultScheduleEvent();*/
	}

	public void onEventSelect(SelectEvent selectEvent) {//SelectEvent selectEvent
		ScheduleEvent se = (ScheduleEvent) selectEvent.getObject();
		this.appointment = (Appointment) se.getData();
	}
	
	public void onEventTypeSelect() {
		if(this.appointment instanceof Appointment) {
			this.type = AppointmentType.GENERIC_APPOINTMENT;
			this.availableParticipants = researcherDAO.findAll();
		} else if (this.appointment instanceof ConferenceAppointment) {
			this.type=AppointmentType.CONFERENCE_APPOINTMENT;
			this.availableParticipants = researcherDAO.findAll();
		} else if (this.appointment instanceof TeachingAppointment){
			this.type=AppointmentType.TEACHING_APPOINTMENT;
			this.availableParticipants = new ArrayList<Researcher>();
		} else if (this.appointment instanceof ResearchGroupMeeting) {
			this.type = AppointmentType.RESEARCH_GROUP_MEETING;
			this.availableParticipants = new ArrayList<Researcher>();
			List<GroupDetails> gs = groupManager.getGroupDetails(credentials.getResearcher());
			for (GroupDetails g : gs) {
				if (g.getType()==GroupType.RESEARCH_GROUP) {
					Set<BigInteger> gmi = g.getMembers();
					for (BigInteger gm: gmi) {
						this.availableParticipants.add(researcherDAO.read(gm));
					}
				}
			}
		} else if (this.appointment instanceof ProjectGroupMeeting) {
			this.type=AppointmentType.PROJECT_GROUP_MEETING;
			this.availableParticipants = new ArrayList<Researcher>();
			List<GroupDetails> gs = groupManager.getGroupDetails(credentials.getResearcher());
			for (GroupDetails g : gs) {
				if (g.getType()==GroupType.PROJECT_GROUP) {
					Set<BigInteger> gmi = g.getMembers();
					for (BigInteger gm: gmi) {
						this.availableParticipants.add(researcherDAO.read(gm));
					}
				}
			}
		}
		
	}

	public void onDateSelect(SelectEvent selectEvent) {
		this.appointment = new Appointment();
		Date date =(Date) selectEvent.getObject(); 
		this.appointment.setStart(date);
		this.appointment.setEnd(date);
	}

	public void onEventMove(ScheduleEntryMoveEvent event) {
		/*FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
				"Event moved", "Day delta:" + event.getDayDelta()
						+ ", Minute delta:" + event.getMinuteDelta());

		addMessage(message);*/
	}

	public void onEventResize(ScheduleEntryResizeEvent event) {
		/*FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
				"Event resized", "Day delta:" + event.getDayDelta()
						+ ", Minute delta:" + event.getMinuteDelta());

		addMessage(message);*/
	}

	private void addMessage(FacesMessage message) {
		FacesContext.getCurrentInstance().addMessage(null, message);
	}
	
	public void createAppointment(){
		//createAppointment.createAppointment(rid, type, groupId, location, description);
	}
	
	public void deleteAppiontment(){
		//deleteAppointment.deleteAppointment(researcherId, appointmentId);
	}



	
	public ScheduleModel getEventModel() {
		return eventModel;
	}
	
	public void setEventModel(ScheduleModel eventModel) {
		this.eventModel = eventModel;
	}

	public CreateAppointment getCreateAppointment() {
		return createAppointment;
	}

	public void setCreateAppointment(CreateAppointment createAppointment) {
		this.createAppointment = createAppointment;
	}

	public DeleteAppointment getDeleteAppointment() {
		return deleteAppointment;
	}

	public void setDeleteAppointment(DeleteAppointment deleteAppointment) {
		this.deleteAppointment = deleteAppointment;
	}


	public void setLazyEventModel(ScheduleModel lazyEventModel) {
		this.lazyEventModel = lazyEventModel;
	}

	public List<Researcher> getAvailableParticipants() {
		return availableParticipants;
	}

	public void setAvailableParticipants(List<Researcher> availableParticipants) {
		this.availableParticipants = availableParticipants;
	}
	
	public void updateAvailableParticipants() {
		if(this.type == AppointmentType.GENERIC_APPOINTMENT) {
			this.availableParticipants = researcherDAO.findAll();
		} else if (this.type == AppointmentType.CONFERENCE_APPOINTMENT) {
			this.availableParticipants = researcherDAO.findAll();
		} else if (this.type == AppointmentType.TEACHING_APPOINTMENT) {
			this.availableParticipants = new ArrayList<Researcher>();
		} else if (this.type == AppointmentType.RESEARCH_GROUP_MEETING) {
			this.availableParticipants = new ArrayList<Researcher>();
			List<GroupDetails> gs = groupManager.getGroupDetails(credentials.getResearcher());
			for (GroupDetails g : gs) {
				if (g.getType()==GroupType.RESEARCH_GROUP) {
					Set<BigInteger> gmi = g.getMembers();
					for (BigInteger gm: gmi) {
						this.availableParticipants.add(researcherDAO.read(gm));
					}
				}
			}
		}
	}
	
	public Credentials getCredentials() {
		return credentials;
	}


	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}


	public AppointmentDAO getAppointmentDAO() {
		return appointmentDAO;
	}


	public void setAppointmentDAO(AppointmentDAO appointmentDAO) {
		this.appointmentDAO = appointmentDAO;
	}


	public GroupManager getGroupManager() {
		return groupManager;
	}


	public void setGroupManager(GroupManager groupManager) {
		this.groupManager = groupManager;
	}


	public ResearcherDAO getResearcherDAO() {
		return researcherDAO;
	}


	public void setResearcherDAO(ResearcherDAO researcherDAO) {
		this.researcherDAO = researcherDAO;
	}


	public ResearcherManager getResearcherManager() {
		return researcherManager;
	}


	public void setResearcherManager(ResearcherManager researcherManager) {
		this.researcherManager = researcherManager;
	}


	public AppointmentManager getAppointmentManager() {
		return appointmentManager;
	}


	public void setAppointmentManager(AppointmentManager appointmentManager) {
		this.appointmentManager = appointmentManager;
	}


	public ScheduleEvent getEvent() {
		return event;
	}


	public void setEvent(ScheduleEvent event) {
		this.event = event;
	}


	public ScheduleModel getLazyEventModel() {
		return lazyEventModel;
	}


	public List<Researcher> getSelectedParticipants() {
		return selectedParticipants;
	}


	public void setSelectedParticipants(List<Researcher> selectedParticipants) {
		this.selectedParticipants = selectedParticipants;
	}


	public List<String> getSelectedParticipantsNames() {
		return selectedParticipantsNames;
	}


	public void setSelectedParticipantsNames(
			List<String> selectedParticipantsNames) {
		this.selectedParticipantsNames = selectedParticipantsNames;
	}
	

}