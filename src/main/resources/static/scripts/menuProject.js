const hamburger = document.getElementById('hamburger');
const navLinks = document.getElementById('navLinks');
const sendChatBtn = document.querySelector('.chat-input span');
const chatInput = document.querySelector('.chat-input textarea');
let userMessage;
const chatbox = document.querySelector('.chatbox');
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

const createChatLi = (message, className) => {
    const chatLi = document.createElement('li');
    chatLi.classList.add('chat', className);
    let chatContent =
        className === 'outgoing'
            ? `<p></p>`
            : `<span
            ><i class="fa-solid fa-robot material-symbols-outlined"></i
          ></span><p></p>`;
    chatLi.innerHTML = chatContent;
    chatLi.querySelector('p').textContent = message;
    return chatLi;
};

const generateResponse = (incomingChatLi) => {
    const messageElement = incomingChatLi.querySelector('p');

    // Send the request to the OpenAI API
    fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: userMessage }),
    })
        .then((res) => res.json())
        .then((data) => {
            if (data.choices) {
                messageElement.textContent = data.choices[0].message.content;
            } else {
                messageElement.textContent = '⚠️ Error: Unable to get response.';
            }
        })
        .catch(() => {
            messageElement.textContent =
                'Oops! Something went wrong. Please try again.';
        })
        .finally(() => {
            chatbox.scrollTo(0, chatbox.scrollHeight);
        });
};

const handleChat = () => {
    userMessage = chatInput.value.trim();
    if (!userMessage) return;
    chatInput.value = '';

    chatbox.appendChild(createChatLi(userMessage, 'outgoing'));
    chatbox.scrollTo(0, chatbox.scrollHeight);

    setTimeout(() => {
        const incomingChatLi = createChatLi('Thinking...', 'incoming');
        chatbox.appendChild(incomingChatLi);
        chatbox.scrollTo(0, chatbox.scrollHeight);
        generateResponse(incomingChatLi);
    }, 600);
};

sendChatBtn.addEventListener('click', handleChat);
