const hamburger = document.getElementById('hamburger');
const navLinks = document.getElementById('navLinks');
const sideBar = document.getElementById('sideBar');
const toggleBtn = document.getElementById('menuToggleBtn');
const body = document.body;

hamburger.addEventListener('click', (e) => {
    e.stopPropagation();
    hamburger.classList.toggle('active');
    navLinks.classList.toggle('active');

    if (navLinks.classList.contains('active')) {
        body.style.overflow = 'hidden';
    } else {
        body.style.overflow = 'auto';
    }
});

document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && navLinks.classList.contains('active')) {
        hamburger.classList.remove('active');
        navLinks.classList.remove('active');
        body.style.overflow = 'auto';
    }
});

document.addEventListener('click', (e) => {
    if (
        navLinks.classList.contains('active') &&
        !navLinks.contains(e.target) &&
        !hamburger.contains(e.target)
    ) {
        hamburger.classList.remove('active');
        navLinks.classList.remove('active');
        body.style.overflow = 'auto';
    }
});

document.querySelectorAll('.nav-links a').forEach((link) => {
    link.addEventListener('click', () => {
        hamburger.classList.remove('active');
        navLinks.classList.remove('active');
        body.style.overflow = '';
    });
});
