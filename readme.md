# Famazing

## What is it?

Android application to coordinate family members (and not only). It has features such as shared tasks, members' location, emergency service with instant notifications and alerts history, family members recap.

## Why did I make it?

Final project for the <strong>Mobile and social computing</strong> course at <i>University of Oulu(FI)</i> during academic year 2015-2016, my Erasmus+ period there.

## About the application

I was experienced in Java, but not in Android development. That was my first try, and didn't go that bad. In the end, I scored third, and first considering only the code quality. Of course what it's bad is the UI, but I can't do that about it.

## A bit of technical details

Basically I have had few time to implement the whole app, also because of other courses and other projects at the same time.
My initial idea was to implement a mini REST server with which exchange messages, performing computation on server-side instead on client side. But time was running out, and the only solution I found, was a free MySQL service, so basically I implemented a DAO class which simulates the operations that the hypotetical server should have made available.
As regard the features, we have:
<ul>
<li><i><strong>Members' locations</strong></i>: there is a service which starts as the user logs into the application and, if he doesn't log out, which also starts at boot time that get GPS coordinates every x seconds (that could be also set with a slider in the settings menu, I know...) and send them to the server to store them and make them available to other users. The same service also fetches, at a different rate, positions of the other family members to show them on the map, if the map screen is open. Of course, if there is no internet connectivity, the whole service gets useless.</li>
<li><i><strong>Alert function</strong></i>: another service which is started and stopped in the same way as the previous one. It starts when the user logs into the app or turns on the phone, and it gets stopped when the phone is turned of (of course) and when the user logs out from the app. Via the settings menu, it's possible to set an emergency number (may be different for each family member) that sends an alert (if there is internet connectivity) to all the other members of the family, showing a notification and, if clicked, opening directly the map view with a pin on the position of the member who sent the alert. This feature is always available in the phone, because I though that since it's used in emergencies, users may not have time to open the app and go anywhere to send the alert, so just make a call to the number (which will be closed) and <strong>wait for help!</strong>...</li>
<li><i><strong>Shared tasks</strong></i>: every member of the family can give a task to any other member, or leave it without completer so that the task could be completed by anyone. Every task has a title and a creator, and can have a place (pinned on the map) with its name, a completer, and an expiration date.</li>
<li><i><strong>Family and alerts recap</strong></i>: there are other two list views that show details of family members and alerts history, giving chanche to click on them and show them into the map.</li>
</ul>
When the application is opened, the home page shows the next task for the user logged in, since I though that that would be the most used feauture, and it doesn't require to go anywhere else to get the searched information.

<strong> The application is fully working, the only required thing to change is formed by the DB credentials, which have been obscured, into the class data/DAO.java</strong>

## Links

Link to <a href = "https://www.youtube.com/watch?v=cNuH0bFzG6g">youtube video</a>