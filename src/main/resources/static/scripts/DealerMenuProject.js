document.addEventListener("DOMContentLoaded", function (){
   const sideBar = document.getElementById("sideBar");
   const toggleBtn = document.getElementById("menuToggleBtn");
   const body = document.body;

   if (toggleBtn && sideBar){
       toggleBtn.addEventListener('click', function(e){
          e.stopPropagation();
          sideBar.classList.toggle("active");
       });

       document.addEventListener('click', function (e){
          if (!sideBar.contains(e.target) && !toggleBtn.contains(e.target)){
              sideBar.classList.remove("active");
          }
       });
       document.addEventListener('keydown', function(e){
           if (e.key === "Escape"){
               sideBar.classList.remove("active");
           }
       });
   }
});