import { useEffect, useMemo, useState } from 'react'
import {
  Activity, ArrowRight, Bell, CalendarDays, Check, ChevronDown,
  ChevronRight, Clock3, FileText, HeartPulse, LayoutDashboard, LogOut,
  Menu, MoreHorizontal, Plus, Search, ShieldCheck, Sparkles, Stethoscope,
  Users, Wallet, X, Send, Pill, Download,
} from 'lucide-react'

const API = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')
const STORAGE_KEY = 'careflow-auth'
const allNavigation = [
  ['Dashboard', LayoutDashboard, ['ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT']],
  ['Patients', Users, ['ADMIN', 'DOCTOR', 'RECEPTIONIST']],
  ['Doctors', Stethoscope, ['ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT']],
  ['Appointments', CalendarDays, ['ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT']],
  ['Medical records', FileText, ['ADMIN', 'DOCTOR']],
  ['Prescriptions', Pill, ['ADMIN', 'DOCTOR']],
  ['Billing', Wallet, ['ADMIN', 'RECEPTIONIST']],
  ['AI assistant', Sparkles, ['ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT']],
  ['Account', ShieldCheck, ['ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT']],
]

const today = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}
const dateLabel = (date) => date ? new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
const timeLabel = (time) => time ? new Date(`2000-01-01T${time}`).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', hour12: true }) : '—'
const initials = (name = '') => name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
const titleCase = (value = '') => value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase())

function storedSession() {
  try {
    return JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    sessionStorage.removeItem(STORAGE_KEY)
    return null
  }
}

async function request(path, options = {}) {
  const session = storedSession()
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {}),
      ...options.headers,
    },
  })
  if (!response.ok) {
    const detail = (await response.text()).trim()
    throw new Error(detail || (response.status === 403
      ? 'Your account does not have permission to perform this action.'
      : `The hospital server returned ${response.status}.`))
  }
  if (response.status === 204) return null
  return response.json()
}

function IconButton({ children, label, onClick, className = '' }) {
  return <button className={`icon-button ${className}`} aria-label={label} onClick={onClick}>{children}</button>
}
function Avatar({ name, tone = 'blue', small = false }) {
  return <span className={`avatar ${tone} ${small ? 'small' : ''}`}>{initials(name)}</span>
}
function Badge({ children }) {
  return <span className={`badge ${String(children).toLowerCase().replaceAll(' ', '-')}`}>{children}</span>
}

export default function App() {
  const [session, setSession] = useState(storedSession)
  const [screen, setScreen] = useState(() => storedSession() ? 'app' : 'home')
  const [authMode, setAuthMode] = useState('login')
  const [page, setPage] = useState('Dashboard')
  const [patients, setPatients] = useState([])
  const [doctors, setDoctors] = useState([])
  const [appointments, setAppointments] = useState([])
  const [records, setRecords] = useState([])
  const [prescriptions, setPrescriptions] = useState([])
  const [invoices, setInvoices] = useState([])
  const [currentPatient, setCurrentPatient] = useState(null)
  const [query, setQuery] = useState('')
  const [mobileNav, setMobileNav] = useState(false)
  const [modal, setModal] = useState('')
  const [assistantOpen, setAssistantOpen] = useState(false)
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState([{ from: 'ai', text: 'Hello! Ask me about hospital services or approved policies.' }])
  const [toast, setToast] = useState('')
  const [apiError, setApiError] = useState('')
  const [busy, setBusy] = useState(false)
  const [reload, setReload] = useState(0)

  const user = session?.user
  const role = user?.role
  const navigation = useMemo(() => allNavigation.filter(([, , roles]) => roles.includes(role)), [role])
  const filteredPatients = useMemo(() => patients.filter((patient) =>
    `${patient.fullName} ${patient.id} ${patient.phone || ''} ${patient.email || ''}`.toLowerCase().includes(query.toLowerCase())), [patients, query])
  const filteredDoctors = useMemo(() => doctors.filter((doctor) =>
    `${doctor.fullName} ${doctor.specialization}`.toLowerCase().includes(query.toLowerCase())), [doctors, query])
  const filteredAppointments = useMemo(() => appointments.filter((item) =>
    `${item.patientName || item.patient?.fullName} ${item.doctorName || item.doctor?.fullName} ${item.department}`.toLowerCase().includes(query.toLowerCase())), [appointments, query])
  const notify = (message) => { setToast(message); window.setTimeout(() => setToast(''), 3200) }

  useEffect(() => {
    if (!session?.token || !user?.role) return undefined
    let live = true
    const patientMode = role === 'PATIENT'
    const requests = [
      request(patientMode ? '/patients/me' : '/patients'),
      request('/doctors'),
      request('/appointments'),
      ['ADMIN', 'DOCTOR'].includes(role) ? request('/medical-records') : Promise.resolve([]),
      ['ADMIN', 'DOCTOR'].includes(role) ? request('/prescriptions') : Promise.resolve([]),
      ['ADMIN', 'RECEPTIONIST'].includes(role) ? request('/invoices') : Promise.resolve([]),
    ]
    Promise.all(requests).then(([patientData, doctorData, appointmentData, recordData, prescriptionData, invoiceData]) => {
      if (!live) return
      const loadedPatients = patientMode ? [patientData] : patientData
      if (!Array.isArray(loadedPatients) || !Array.isArray(doctorData) || !Array.isArray(appointmentData)) {
        throw new Error('The hospital server returned an unexpected collection format.')
      }
      setPatients(loadedPatients)
      setCurrentPatient(patientMode ? patientData : null)
      setDoctors(doctorData)
      setAppointments(appointmentData)
      setRecords(recordData)
      setPrescriptions(prescriptionData)
      setInvoices(invoiceData)
      setApiError('')
    }).catch((error) => {
      if (live) setApiError(error.message)
    })
    return () => { live = false }
  }, [session, role, reload])

  async function authenticate(event) {
    event.preventDefault()
    setBusy(true)
    const data = Object.fromEntries(new FormData(event.currentTarget))
    try {
      const result = await request(`/auth/${authMode}`, { method: 'POST', body: JSON.stringify(data) })
      if (!result?.token || !result?.user?.role) throw new Error('The server returned an invalid sign-in response.')
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(result))
      setSession(result)
      setScreen('app')
      setPage('Dashboard')
    } catch (error) {
      notify(error.message)
    } finally {
      setBusy(false)
    }
  }

  function logout() {
    sessionStorage.removeItem(STORAGE_KEY)
    setSession(null)
    setScreen('home')
    setPage('Dashboard')
    setPatients([])
    setDoctors([])
    setAppointments([])
    setRecords([])
    setPrescriptions([])
    setInvoices([])
  }

  async function createPatient(event) {
    event.preventDefault()
    setBusy(true)
    const data = Object.fromEntries(new FormData(event.currentTarget))
    try {
      await request('/patients', {
        method: 'POST',
        body: JSON.stringify({ ...data, dateOfBirth: data.dateOfBirth || null }),
      })
      setReload((value) => value + 1)
      setModal('')
      notify('Patient registered successfully.')
    } catch (error) {
      notify(error.message)
    } finally {
      setBusy(false)
    }
  }

  async function createAppointment(event) {
    event.preventDefault()
    setBusy(true)
    const data = Object.fromEntries(new FormData(event.currentTarget))
    try {
      await request('/appointments', {
        method: 'POST',
        body: JSON.stringify({
          patientId: role === 'PATIENT' ? currentPatient.id : Number(data.patientId),
          doctorId: Number(data.doctorId),
          department: data.department,
          appointmentDate: data.appointmentDate,
          appointmentTime: data.appointmentTime,
        }),
      })
      setReload((value) => value + 1)
      setModal('')
      notify('Appointment booked successfully.')
    } catch (error) {
      notify(error.message)
    } finally {
      setBusy(false)
    }
  }

  async function askAssistant(event) {
    event.preventDefault()
    const prompt = question.trim()
    if (!prompt || busy) return
    setQuestion('')
    setMessages((current) => [...current, { from: 'user', text: prompt }])
    setBusy(true)
    try {
      const answer = await request('/assistant/ask', { method: 'POST', body: JSON.stringify({ question: prompt }) })
      setMessages((current) => [...current, { from: 'ai', text: answer.answer, sources: answer.sources || [] }])
    } catch (error) {
      setMessages((current) => [...current, { from: 'ai', text: `I can’t reach the hospital assistant right now: ${error.message}` }])
    } finally {
      setBusy(false)
    }
  }

  if (screen === 'home') {
    return <LandingPage onLogin={() => { setAuthMode('login'); setScreen('auth') }} onRegister={() => { setAuthMode('register'); setScreen('auth') }} />
  }
  if (screen === 'auth') {
    return <AuthPage mode={authMode} busy={busy} onMode={setAuthMode} onSubmit={authenticate} onBack={() => setScreen('home')} toast={toast} />
  }

  const go = (label) => { setPage(label); setQuery(''); setMobileNav(false); if (label === 'AI assistant') setAssistantOpen(true) }
  const appointmentsToday = appointments.filter((item) => item.appointmentDate === today())
  const roleLabel = titleCase(role || 'patient')

  return <div className="app-shell">
    <aside className={`sidebar ${mobileNav ? 'open' : ''}`}>
      <div className="brand" onClick={() => go('Dashboard')}><span className="brand-mark"><HeartPulse size={21} /></span><span>Careflow<small>HOSPITAL MANAGEMENT</small></span><IconButton label="Close navigation" className="mobile-close" onClick={() => setMobileNav(false)}><X size={18} /></IconButton></div>
      <div className="workspace"><span className="workspace-icon"><Activity size={17} /></span><span><b>Hospital workspace</b><small>{roleLabel} access</small></span><ChevronDown size={14} /></div>
      <div className="nav-label">WORKSPACE</div>
      <nav className="nav-list">{navigation.map(([label, Icon]) => <button key={label} className={`nav-item ${page === label ? 'active' : ''}`} onClick={() => go(label)}><Icon size={17} /><span>{label}</span>{label === 'Appointments' && <span className="nav-count">{appointments.length}</span>}</button>)}</nav>
      <div className="sidebar-bottom"><div className="help-card"><span><ShieldCheck size={17} /></span><div><b>Care, connected.</b><p>Hospital data comes from your secured account.</p></div></div><div className="profile"><Avatar name={user.fullName} tone="teal" /><div><b>{user.fullName}</b><small>{roleLabel}</small></div><IconButton label="Sign out" onClick={logout}><LogOut size={16} /></IconButton></div></div>
    </aside>
    {mobileNav && <button className="mobile-overlay" aria-label="Close navigation" onClick={() => setMobileNav(false)} />}
    <main className="main-content">
      <header className="topbar"><div className="topbar-left"><IconButton label="Open navigation" className="menu-toggle" onClick={() => setMobileNav(true)}><Menu size={20} /></IconButton><div className="breadcrumb">Careflow <ChevronRight size={14} /> <b>{page}</b></div></div><div className="topbar-actions"><label className="global-search"><Search size={16} /><input aria-label="Search this page" placeholder="Search records..." value={query} onChange={(event) => setQuery(event.target.value)} /></label><span className="top-divider" /><span className={`api-status ${apiError ? '' : 'connected'}`}><i />{apiError ? 'API unavailable' : 'Secure API connected'}</span><IconButton label="Notifications" onClick={() => notify('You’re all caught up.')}><Bell size={18} /></IconButton><Avatar name={user.fullName} tone="teal" small /></div></header>
      <div className="page-wrap">
        {apiError && <div className="api-error" role="alert"><span><b>Unable to load hospital data.</b><small>{apiError} Start MySQL and the Spring Boot API, then retry.</small></span><button className="outline-button" onClick={() => setReload((value) => value + 1)}>Retry</button></div>}
        {page === 'Dashboard' && <Dashboard user={user} role={role} patients={patients} doctors={doctors} appointments={appointments} invoices={invoices} onPage={go} onBook={() => setModal('appointment')} onAI={() => setAssistantOpen(true)} />}
        {page === 'Patients' && <PatientsPage patients={filteredPatients} onAdd={() => setModal('patient')} />}
        {page === 'Doctors' && <DoctorsPage doctors={filteredDoctors} />}
        {page === 'Appointments' && <AppointmentsPage appointments={filteredAppointments} onAdd={() => setModal('appointment')} />}
        {page === 'Medical records' && <RecordsPage records={records} />}
        {page === 'Prescriptions' && <PrescriptionsPage prescriptions={prescriptions} />}
        {page === 'Billing' && <BillingPage invoices={invoices} />}
        {page === 'AI assistant' && <AssistantIntro onOpen={() => setAssistantOpen(true)} />}
        {page === 'Account' && <AccountPage user={user} onLogout={logout} />}
        <footer className="page-footer"><span>© 2026 Careflow Hospital Management</span><span>Role-based access <ShieldCheck size={13} /></span></footer>
      </div>
    </main>
    {modal && <Modal kind={modal} busy={busy} role={role} patient={currentPatient} patients={patients} doctors={doctors} onClose={() => setModal('')} onPatient={createPatient} onAppointment={createAppointment} />}
    {assistantOpen && <Assistant messages={messages} question={question} busy={busy} onQuestion={setQuestion} onSubmit={askAssistant} onClose={() => setAssistantOpen(false)} />}
    {toast && <div className="toast"><Check size={15} />{toast}</div>}
    {role !== 'PATIENT' && <button className="floating-ai" onClick={() => setAssistantOpen(true)} aria-label="Open AI assistant"><Sparkles size={18} /><span>AI assistant</span></button>}
  </div>
}

function LandingPage({ onLogin, onRegister }) {
  const features = [
    [Users, 'Patient records', 'Maintain patient profiles from a secure, searchable directory.'],
    [CalendarDays, 'Appointment scheduling', 'Coordinate patient visits with a live schedule.'],
    [ShieldCheck, 'Role-based accounts', 'Separate access for administrators, doctors, receptionists, and patients.'],
    [Sparkles, 'Hospital information assistant', 'Ask questions about approved hospital guidance.'],
  ]
  return <main className="public-page">
    <header className="public-header"><a className="public-brand" href="#home"><span className="brand-mark"><HeartPulse size={21} /></span>Careflow</a><div><button className="public-link" onClick={onLogin}>Log in</button><button className="primary-button" onClick={onRegister}>Create account <ArrowRight size={15} /></button></div></header>
    <section className="hero" id="home"><span className="hero-eyebrow"><ShieldCheck size={15} /> HOSPITAL OPERATIONS, CONNECTED</span><h1>A simpler way to<br /><span>care for every patient.</span></h1><p>One secure workspace for hospital teams and patients. Schedule visits, coordinate care, and find approved hospital information in one place.</p><div className="hero-actions"><button className="primary-button" onClick={onRegister}>Create a patient account <ArrowRight size={16} /></button><button className="outline-button" onClick={onLogin}>Sign in</button></div><div className="hero-note"><ShieldCheck size={15} /> Secure, role-based access for your hospital community</div></section>
    <section className="features-section"><div className="features-heading"><span>BUILT FOR EVERYDAY CARE</span><h2>Everything your team needs to stay in sync.</h2></div><div className="features-grid">{features.map(([Icon, title, description]) => <article className="feature-card" key={title}><span><Icon size={20} /></span><h3>{title}</h3><p>{description}</p></article>)}</div></section>
    <footer className="public-footer"><span>Careflow · Hospital Management</span><button className="public-link" onClick={onLogin}>Staff sign in <ArrowRight size={14} /></button></footer>
  </main>
}

function AuthPage({ mode, busy, onMode, onSubmit, onBack, toast }) {
  const isRegister = mode === 'register'
  return <main className="auth-page"><button className="public-brand auth-back" onClick={onBack}><span className="brand-mark"><HeartPulse size={21} /></span>Careflow</button>
    <section className="auth-card"><span className="auth-icon"><ShieldCheck size={22} /></span><p className="auth-kicker">SECURE HOSPITAL ACCESS</p><h1>{isRegister ? 'Create your account' : 'Welcome back'}</h1><p className="auth-description">{isRegister ? 'Create a patient account to schedule and manage your appointments.' : 'Sign in to your hospital workspace.'}</p>
      <form className="auth-form" onSubmit={onSubmit}>
        {isRegister && <label>Full name<input name="fullName" type="text" autoComplete="name" required minLength={2} maxLength={120} placeholder="Your full name" /></label>}
        <label>Email address<input name="email" type="email" autoComplete="email" required maxLength={254} placeholder="name@mail.com" /></label>
        <label>Password<input name="password" type="password" autoComplete={isRegister ? 'new-password' : 'current-password'} required minLength={6} maxLength={72} placeholder={isRegister ? 'At least 6 characters' : 'Enter your password'} /><span className="password-hint">{isRegister ? 'Use at least 6 characters. No special character rules.' : ''}</span></label>
        <button className="primary-button auth-submit" type="submit" disabled={busy}>{busy ? 'Please wait…' : isRegister ? 'Create account' : 'Sign in'} {!busy && <ArrowRight size={15} />}</button>
      </form>
      <p className="auth-switch">{isRegister ? 'Already have an account?' : 'New to Careflow?'} <button className="public-link" onClick={() => onMode(isRegister ? 'login' : 'register')}>{isRegister ? 'Sign in' : 'Create a patient account'}</button></p>
      {!isRegister && <details className="local-credentials"><summary>Local development sign-in accounts</summary><p>All starter accounts use <code>123456</code> as the password. Development only.</p><span>admin@mail.com · doctor@mail.com</span><span>receptionist@mail.com · patient@mail.com</span></details>}
      {toast && <div className="auth-error" role="alert">{toast}</div>}
    </section><p className="auth-safety"><ShieldCheck size={14} /> Protected by role-based hospital access</p>
  </main>
}

function Dashboard({ user, role, patients, doctors, appointments, invoices, onPage, onBook, onAI }) {
  const todayAppointments = appointments.filter((item) => item.appointmentDate === today())
  const patientMode = role === 'PATIENT'
  const pendingInvoices = invoices.filter((invoice) => ['PENDING', 'OVERDUE'].includes(invoice.status)).length
  const metrics = patientMode
    ? [
        ['My appointments', appointments.length, CalendarDays, 'blue'],
        ['Upcoming visits', appointments.filter((item) => item.appointmentDate >= today()).length, Clock3, 'violet'],
        ['Hospital doctors', doctors.length, Stethoscope, 'green'],
      ]
    : [
        ['Patients', patients.length, Users, 'blue'],
        ['Doctors', doctors.length, Stethoscope, 'green'],
        ['Appointments today', todayAppointments.length, CalendarDays, 'violet'],
        ...(role !== 'DOCTOR' ? [['Outstanding invoices', pendingInvoices, Wallet, 'orange']] : []),
      ]
  return <>
    <PageHeading eyebrow="LIVE HOSPITAL DATA" title={`Welcome, ${user.fullName.split(' ')[0]}`} description={patientMode ? 'View the care team and keep track of your appointments.' : 'Your hospital overview, updated from live records.'} action={role !== 'DOCTOR' || patientMode ? 'Book appointment' : undefined} onAction={onBook} />
    {metrics.length > 0 && <section className="metrics-grid">{metrics.map(([title, value, Icon, tone]) => <Metric key={title} title={title} value={value} icon={<Icon size={18} />} tone={tone} />)}</section>}
    <section className="dashboard-grid"><div className="panel chart-panel"><PanelHead title={patientMode ? 'My appointment schedule' : 'Today’s appointment schedule'} subtitle={`${todayAppointments.length} scheduled for ${dateLabel(today())}`} action="View all" onAction={() => onPage('Appointments')} /><AppointmentsTable appointments={patientMode ? appointments : todayAppointments} /></div><div className="panel overview-panel"><PanelHead title="Care team" subtitle={`${doctors.length} doctor records`} action="View team" onAction={() => onPage('Doctors')} /><div className="overview-list">{doctors.slice(0, 4).map((doctor) => <OverviewStat key={doctor.id} icon={<Stethoscope size={16} />} label={doctor.fullName} value={doctor.specialization} color="blue" />)}{doctors.length === 0 && <Empty label="No doctors in the directory yet." />}</div><button className="assistant-promo" onClick={onAI}><span><Sparkles size={17} /></span><b>Ask the hospital assistant</b><ArrowRight size={15} /></button></div></section>
    {!patientMode && <section className="panel table-panel recent-patients"><PanelHead title="Recently registered patients" subtitle={`${patients.length} patient records`} action="All patients" onAction={() => onPage('Patients')} /><PatientsTable patients={patients.slice(0, 5)} /></section>}
  </>
}

function Metric({ title, value, icon, tone }) {
  return <article className="metric-card"><div className="metric-top"><span>{title}</span><span className={`metric-icon ${tone}`}>{icon}</span></div><div className="metric-value">{Number(value).toLocaleString()}</div><div className="metric-bottom"><span>From hospital records</span></div></article>
}
function PanelHead({ title, subtitle, action, onAction }) {
  return <div className="panel-header"><div><h2>{title}</h2><p>{subtitle}</p></div>{action && <button className="text-button" onClick={onAction}>{action}<ArrowRight size={14} /></button>}</div>
}
function OverviewStat({ icon, label, value, color }) {
  return <div className="overview-stat"><span className={`overview-icon ${color}`}>{icon}</span><span>{label}</span><b>{value}</b></div>
}
function PatientsTable({ patients }) {
  return <div className="table-scroll"><table><thead><tr><th>PATIENT</th><th>AGE</th><th>GENDER</th><th>PHONE</th><th>STATUS</th></tr></thead><tbody>{patients.map((patient) => <tr key={patient.id}><td><div className="patient-cell"><Avatar name={patient.fullName} small /><span><b>{patient.fullName}</b><small>#{patient.id}</small></span></div></td><td>{ageOf(patient.dateOfBirth)}</td><td>{patient.gender || '—'}</td><td>{patient.phone || '—'}</td><td><Badge>{patient.status || 'Active'}</Badge></td></tr>)}</tbody></table>{patients.length === 0 && <Empty label="No patient records found." />}</div>
}
function AppointmentsTable({ appointments }) {
  return <div className="table-scroll"><table><thead><tr><th>PATIENT</th><th>DOCTOR</th><th>DEPARTMENT</th><th>DATE & TIME</th><th>STATUS</th></tr></thead><tbody>{appointments.map((item) => <tr key={item.id}><td>{item.patientName || item.patient?.fullName}</td><td>{item.doctorName || item.doctor?.fullName}</td><td>{item.department}</td><td>{dateLabel(item.appointmentDate)} · {timeLabel(item.appointmentTime)}</td><td><Badge>{titleCase(item.status)}</Badge></td></tr>)}</tbody></table>{appointments.length === 0 && <Empty label="No appointments found." />}</div>
}
function PatientsPage({ patients, onAdd }) {
  return <><PageHeading title="Patients" description="View and register patient profiles." action="Add patient" onAction={onAdd} /><section className="panel table-panel"><div className="list-toolbar"><div><h2>Patient directory <span className="count-pill">{patients.length}</span></h2><p>Patient records from the database</p></div></div><PatientsTable patients={patients} /></section></>
}
function DoctorsPage({ doctors }) {
  return <><PageHeading title="Doctors" description="The hospital care team and specialties." /><DataTable title="Medical team" subtitle={`${doctors.length} doctor records`} columns={['DOCTOR', 'SPECIALIZATION', 'EMAIL', 'PHONE', 'STATUS']} rows={doctors.map((doctor) => [doctor.fullName, doctor.specialization, doctor.email, doctor.phone, titleCase(doctor.status)])} /></>
}
function AppointmentsPage({ appointments, onAdd }) {
  const confirmed = appointments.filter((item) => item.status === 'CONFIRMED').length
  const waiting = appointments.filter((item) => item.status === 'WAITING').length
  return <><PageHeading title="Appointments" description="Schedule and manage visits from hospital records." action="Book appointment" onAction={onAdd} /><div className="metrics-grid short-grid"><Metric title="All appointments" value={appointments.length} icon={<CalendarDays size={18} />} tone="blue" /><Metric title="Confirmed" value={confirmed} icon={<Check size={18} />} tone="green" /><Metric title="Waiting" value={waiting} icon={<Clock3 size={18} />} tone="orange" /><Metric title="Today" value={appointments.filter((item) => item.appointmentDate === today()).length} icon={<Activity size={18} />} tone="violet" /></div><section className="panel table-panel"><PanelHead title="Appointment schedule" subtitle={`${appointments.length} records`} /><AppointmentsTable appointments={appointments} /></section></>
}
function RecordsPage({ records }) {
  return <><PageHeading title="Medical records" description="Clinical visit records for authorized care team members." /><DataTable title="Patient visit history" subtitle={`${records.length} medical records`} columns={['DATE', 'PATIENT', 'DOCTOR', 'DIAGNOSIS', 'NOTES']} rows={records.map((record) => [dateLabel(record.visitDate), record.patient.fullName, record.doctor.fullName, record.diagnosis, record.notes])} /></>
}
function PrescriptionsPage({ prescriptions }) {
  return <><PageHeading title="Prescriptions" description="Medication orders recorded by the care team." /><DataTable title="Prescription records" subtitle={`${prescriptions.length} prescriptions`} columns={['DATE', 'PATIENT', 'MEDICATION', 'DOSAGE', 'PRESCRIBED BY', 'STATUS']} rows={prescriptions.map((prescription) => [dateLabel(prescription.prescribedDate), prescription.patient.fullName, prescription.medication, prescription.dosage, prescription.doctor.fullName, titleCase(prescription.status)])} /></>
}
function BillingPage({ invoices }) {
  return <><PageHeading title="Billing & payments" description="Review patient invoices and payment activity." /><section className="metrics-grid short-grid"><Metric title="Pending payments" value={invoices.filter((invoice) => ['PENDING', 'OVERDUE'].includes(invoice.status)).length} icon={<Clock3 size={18} />} tone="orange" /><Metric title="Paid invoices" value={invoices.filter((invoice) => invoice.status === 'PAID').length} icon={<Wallet size={18} />} tone="green" /><Metric title="All invoices" value={invoices.length} icon={<FileText size={18} />} tone="blue" /></section><DataTable title="Invoice records" subtitle={`${invoices.length} invoices`} columns={['INVOICE', 'PATIENT', 'DATE', 'AMOUNT', 'STATUS']} rows={invoices.map((invoice) => [invoice.invoiceNumber, invoice.patient.fullName, dateLabel(invoice.invoiceDate), Number(invoice.amount).toLocaleString('en-IN', { style: 'currency', currency: 'INR' }), titleCase(invoice.status)])} /></>
}
function DataTable({ title, subtitle, columns, rows }) {
  return <section className="panel table-panel"><PanelHead title={title} subtitle={subtitle} /><div className="table-scroll"><table><thead><tr>{columns.map((column) => <th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, cellIndex) => <td key={cellIndex}>{cellIndex === row.length - 1 && ['Active', 'Completed', 'Paid', 'Pending', 'Confirmed', 'Waiting', 'Overdue'].includes(cell) ? <Badge>{cell}</Badge> : cell}</td>)}</tr>)}</tbody></table>{rows.length === 0 && <Empty label="No records available." />}</div></section>
}
function AccountPage({ user, onLogout }) {
  return <><PageHeading title="Your account" description="Your authenticated hospital account and permissions." /><section className="panel settings-panel account-panel"><Avatar name={user.fullName} tone="teal" /><h2>{user.fullName}</h2><p>{user.email}</p><span className="role-pill">{titleCase(user.role)}</span><button className="outline-button" onClick={onLogout}><LogOut size={15} /> Sign out</button></section></>
}
function PageHeading({ eyebrow = 'HOSPITAL MANAGEMENT', title, description, action, onAction }) {
  return <div className="welcome-row"><div><div className="eyebrow"><i />{eyebrow}</div><h1>{title}</h1><p className="subtitle">{description}</p></div>{action && <button className="primary-button" onClick={onAction}><Plus size={16} />{action}</button>}</div>
}
function AssistantIntro({ onOpen }) {
  return <><PageHeading title="Hospital assistant" description="Ask questions grounded in approved hospital information." /><section className="assistant-welcome"><span><Sparkles size={25} /></span><div><div className="assistant-tag">HOSPITAL KNOWLEDGE ASSISTANT</div><h2>How can I help you today?</h2><p>Ask about hospital services and approved policies. The assistant does not provide diagnoses or patient-specific medical advice.</p><button className="primary-button" onClick={onOpen}>Start a conversation <ArrowRight size={15} /></button></div></section></>
}
function ageOf(date) {
  if (!date) return '—'
  const birth = new Date(`${date}T00:00:00`)
  const now = new Date()
  let age = now.getFullYear() - birth.getFullYear()
  if (now < new Date(now.getFullYear(), birth.getMonth(), birth.getDate())) age--
  return `${age} yrs`
}
function Empty({ label }) { return <div className="empty-state"><Users size={20} />{label}</div> }

function Modal({ kind, busy, role, patient, patients, doctors, onClose, onPatient, onAppointment }) {
  const isPatient = kind === 'patient'
  return <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><section className="modal"><div className="modal-heading"><div><span className="modal-icon">{isPatient ? <Users size={18} /> : <CalendarDays size={18} />}</span><h2>{isPatient ? 'Register patient' : 'Book appointment'}</h2><p>{isPatient ? 'Add a patient to the hospital directory.' : 'Select a doctor and appointment time.'}</p></div><IconButton label="Close dialog" onClick={onClose}><X size={18} /></IconButton></div>
    {isPatient
      ? <form onSubmit={onPatient}><label>Full name<input name="fullName" required minLength={2} maxLength={120} placeholder="Patient full name" /></label><div className="form-row"><label>Email<input name="email" type="email" maxLength={254} placeholder="patient@mail.com" /></label><label>Phone<input name="phone" placeholder="Phone number" /></label></div><div className="form-row"><label>Date of birth<input name="dateOfBirth" type="date" /></label><label>Gender<select name="gender"><option value="">Select</option><option>Female</option><option>Male</option><option>Other</option></select></label></div><label>Blood group<select name="bloodGroup"><option value="">Select</option>{['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((type) => <option key={type}>{type}</option>)}</select></label><div className="modal-actions"><button type="button" className="outline-button" onClick={onClose}>Cancel</button><button className="primary-button" type="submit" disabled={busy}>{busy ? 'Saving…' : 'Register patient'}</button></div></form>
      : <form onSubmit={onAppointment}>{role !== 'PATIENT' && <label>Patient<select name="patientId" required defaultValue=""><option value="" disabled>Select a patient</option>{patients.map((item) => <option key={item.id} value={item.id}>{item.fullName}</option>)}</select></label>}<div className="form-row"><label>Doctor<select name="doctorId" required defaultValue=""><option value="" disabled>Select a doctor</option>{doctors.map((doctor) => <option key={doctor.id} value={doctor.id}>{doctor.fullName} · {doctor.specialization}</option>)}</select></label><label>Department<select name="department" required>{[...new Set(doctors.map((doctor) => doctor.specialization))].map((department) => <option key={department}>{department}</option>)}</select></label></div><div className="form-row"><label>Date<input name="appointmentDate" type="date" min={today()} defaultValue={today()} required /></label><label>Time<input name="appointmentTime" type="time" defaultValue="11:00" required /></label></div><div className="modal-actions"><button type="button" className="outline-button" onClick={onClose}>Cancel</button><button className="primary-button" type="submit" disabled={busy || doctors.length === 0 || (role !== 'PATIENT' && patients.length === 0)}>{busy ? 'Saving…' : 'Confirm appointment'}</button></div></form>}
  </section></div>
}
function Assistant({ messages, question, busy, onQuestion, onSubmit, onClose }) {
  return <div className="assistant-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose() }}><section className="assistant-drawer"><header className="drawer-header"><span className="drawer-bot"><Sparkles size={17} /></span><div><b>Hospital assistant</b><small><i /> Approved information assistant</small></div><IconButton label="Close assistant" onClick={onClose}><X size={18} /></IconButton></header><div className="ai-disclaimer"><ShieldCheck size={15} />Not for emergencies, diagnosis, or patient-specific medical guidance.</div><div className="chat-messages">{messages.map((message, index) => <div key={index} className={`chat-message ${message.from}`}><span className="chat-avatar">{message.from === 'ai' ? <Sparkles size={14} /> : <Users size={14} />}</span><div><p>{message.text}</p>{message.sources?.map((source) => <small className="source-chip" key={source}>{source}</small>)}</div></div>)}{busy && <div className="chat-message"><span className="chat-avatar"><Sparkles size={14} /></span><p>Checking approved information…</p></div>}<div className="suggestions"><small>QUICK QUESTIONS</small><button type="button" onClick={() => onQuestion('What are the visiting hours?')}>What are the visiting hours?</button><button type="button" onClick={() => onQuestion('Where is the cardiology department?')}>Where is the cardiology department?</button></div></div><form className="chat-form" onSubmit={onSubmit}><input value={question} onChange={(event) => onQuestion(event.target.value)} placeholder="Ask about hospital services..." aria-label="Ask the assistant" /><button aria-label="Send question" disabled={busy}><Send size={16} /></button></form><p className="chat-footnote">Answers are limited to the connected hospital knowledge base.</p></section></div>
}
