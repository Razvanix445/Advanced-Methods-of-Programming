package com.example.lab7.service;

import com.example.lab7.Session;
import com.example.lab7.domain.*;
import com.example.lab7.exceptions.*;
import com.example.lab7.repository.FriendRequestRepository;
import com.example.lab7.repository.FriendshipDBPagingRepository;
import com.example.lab7.repository.MessageDBPagingRepository;
import com.example.lab7.repository.Repository;
import com.example.lab7.repository.paging.*;
import com.example.lab7.validators.MessageValidator;
import com.example.lab7.validators.PrietenieValidator;
import com.example.lab7.validators.UtilizatorValidator;
import com.example.lab7.validators.Validator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Service {

    private PagingRepository<Long, Utilizator> repositoryUtilizatori;
    private FriendshipDBPagingRepository repositoryPrietenii;
    private MessageDBPagingRepository repositoryMessages;
    private FriendRequestRepository repositoryFriendRequests;
    private Validator<Utilizator> validatorUtilizator;
    private Validator<Prietenie> validatorPrietenie;
    private Validator<Message> validatorMessage;
    private PageImplementation<Utilizator> pageImplementationUsers;
    private PageImplementation<Utilizator> pageImplementationFriends;
    private PageImplementation<Message> pageImplementationMessages;
    private PageImplementation<FriendRequest> pageImplementationPendingRequests;
    private Page<Utilizator> currentPageUsers;
    private Page<Utilizator> currentPageFriendships;
    private Page<Message> currentPageMessages;
    private Page<FriendRequest> currentPageFriendRequests;

    private static long urmatorulIdDisponibilUtilizatori = 1;
    private static long urmatorulIdDisponibilPrietenii = 1;
    private static long urmatorulIdDisponibilMessages = 1;

    public Service(PagingRepository<Long, Utilizator> utilizatorRepository, FriendshipDBPagingRepository prietenieRepository, MessageDBPagingRepository messageRepository, FriendRequestRepository friendRequestsRepository) {
        this.repositoryUtilizatori = utilizatorRepository;
        this.repositoryPrietenii = prietenieRepository;
        this.repositoryMessages = messageRepository;
        this.repositoryFriendRequests = friendRequestsRepository;
        this.validatorUtilizator = new UtilizatorValidator();
        this.validatorPrietenie = new PrietenieValidator(repositoryUtilizatori);
        this.validatorMessage = new MessageValidator();

        this.pageImplementationUsers = new PageImplementation<>(new PageableImplementation(1, 60), Stream.empty());
        this.pageImplementationFriends = new PageImplementation<>(new PageableImplementation(1, 60), Stream.empty());
        this.pageImplementationMessages = new PageImplementation<>(new PageableImplementation(1, 10), Stream.empty());
        this.pageImplementationPendingRequests = new PageImplementation<>(new PageableImplementation(1, 3), Stream.empty());
        this.currentPageUsers = repositoryUtilizatori.findAll(pageImplementationUsers.getPageable());
        this.currentPageFriendships = repositoryPrietenii.findAll(pageImplementationFriends.getPageable(), 1L);
        this.currentPageMessages = repositoryMessages.findAll(pageImplementationFriends.getPageable(), 1L, 2L);
        this.currentPageFriendRequests = repositoryFriendRequests.findAll(pageImplementationPendingRequests.getPageable(), 1L);
    }

    private Long genereazaIdUnicUtilizator() {
        return urmatorulIdDisponibilUtilizatori++;
    }

    private Long genereazaIdUnicPrietenie() {
        return urmatorulIdDisponibilPrietenii++;
    }

    private Long genereazaIdUnicMessage() {
        return urmatorulIdDisponibilMessages++;
    }

    public Utilizator adaugaUtilizator(Utilizator utilizator) {
        validatorUtilizator.validate(utilizator);
        utilizator.setId(genereazaIdUnicUtilizator());
        repositoryUtilizatori.save(utilizator);
        return utilizator;
    }

    public Utilizator stergeUtilizator(Utilizator utilizator) {
        validatorUtilizator.validate(utilizator);
        List<Long> deSters = new ArrayList<>();

        getAllPrietenii().forEach(prietenie -> {
            if (prietenie.getIdUtilizator1().equals(utilizator.getId()) || prietenie.getIdUtilizator2().equals(utilizator.getId()))
                deSters.add(prietenie.getId());
        });

        deSters.forEach(repositoryPrietenii::delete);
        Optional<Utilizator> utilizatorStersOptional = repositoryUtilizatori.delete(utilizator.getId());

        if (utilizatorStersOptional.isPresent()) {
            Utilizator utilizatorSters = utilizatorStersOptional.get();
            utilizator.getPrieteni().forEach(prieten -> {
                prieten.stergePrieten(utilizatorSters);
            });
            return utilizatorSters;
        } else {
            throw new UtilizatorInexistentException("Utilizatorul nu exista!");
        }
    }

    public Utilizator modificaUtilizator(Utilizator utilizator) {
        validatorUtilizator.validate(utilizator);
        Utilizator utilizatorModificat = new Utilizator(utilizator.getPrenume(), utilizator.getNume(),
                utilizator.getUsername(), utilizator.getPassword(), utilizator.getEmail());
        validatorUtilizator.validate(utilizatorModificat);

        utilizatorModificat.setId(utilizator.getId());
        utilizator.getPrieteni().forEach(prieten -> {
            utilizatorModificat.adaugaPrieten(prieten);
            prieten.getPrieteni().remove(utilizator);
            prieten.getPrieteni().add(utilizatorModificat);
        });
        repositoryUtilizatori.update(utilizatorModificat, utilizator.getId());
        return utilizator;
    }

    public Utilizator cautaUtilizator(Long utilizatorID) {
        return repositoryUtilizatori.findOne(utilizatorID).orElseThrow(() -> new UtilizatorInexistentException("Utilizatorul nu exista!"));
    }

    public Utilizator cautaUtilizatorDupaUsername(String username) {
        Iterable<Utilizator> utilizatori = repositoryUtilizatori.findAll();
        for (Utilizator utilizator : utilizatori) {
            if (utilizator.getUsername().equals(username))
                return utilizator;
        }
        return null;
    }

    public List<Utilizator> cautaUtilizatoriDupaCeva(String data) {
        Iterable<Utilizator> utilizatori = repositoryUtilizatori.findAll();
        return StreamSupport.stream(utilizatori.spliterator(), false)
                .filter(utilizator ->
                        (data.isEmpty() || utilizator.getPrenume().toLowerCase().contains(data.toLowerCase())) ||
                                (data.isEmpty() || utilizator.getNume().toLowerCase().contains(data.toLowerCase())) ||
                                (data.isEmpty() || utilizator.getUsername().toLowerCase().contains(data.toLowerCase()))
                )
                .collect(Collectors.toList());
    }

    public Iterable<Utilizator> getAllUtilizatori() {
        return repositoryUtilizatori.findAll();
    }

    public Page<Utilizator> getAllUtilizatoriPaginat(Pageable pageable) {
        this.pageImplementationUsers = new PageImplementation<>(pageable, Stream.empty());
        this.currentPageUsers = repositoryUtilizatori.findAll(pageImplementationUsers.getPageable());
        return repositoryUtilizatori.findAll(pageable);
    }

    public Page<Utilizator> getAllPrieteniPaginat(Pageable pageable, Long userID) {
        this.pageImplementationFriends = new PageImplementation<>(pageable, Stream.empty());
        this.currentPageFriendships = repositoryPrietenii.findAll(pageImplementationFriends.getPageable(), userID);
        return repositoryPrietenii.findAll(pageable, userID);
    }

    public Page<FriendRequest> getAllPendingRequestsPaginat(Pageable pageable) {
        this.pageImplementationPendingRequests = new PageImplementation<>(pageable, Stream.empty());
        this.currentPageFriendRequests = repositoryFriendRequests.findAll(pageImplementationPendingRequests.getPageable(), getLoggedUser().getId());
        return repositoryFriendRequests.findAll(pageable, getLoggedUser().getId());
    }

    public void acceptFriendRequest(Utilizator friend) {
        Optional<FriendRequest> existentRequest = repositoryFriendRequests.findOne(friend.getId(), getLoggedUser().getId());
        if (existentRequest.isPresent() && !existentRequest.get().getFromUserId().equals(getLoggedUser().getId())) {
            repositoryFriendRequests.updateRequestType(friend.getId(), getLoggedUser().getId(), "approved");
            adaugaPrietenie(getLoggedUser().getId(), friend.getId());
        }
    }

    public void declineFriendRequest(Utilizator friend) {
        Optional<FriendRequest> existentRequest = repositoryFriendRequests.findOne(friend.getId(), getLoggedUser().getId());
        if (existentRequest.isPresent() && !existentRequest.get().getFromUserId().equals(getLoggedUser().getId())) {
            repositoryFriendRequests.updateRequestType(friend.getId(), getLoggedUser().getId(), "declined");
        }
    }

    public void addFriendRequest(Utilizator friend) {
        FriendRequest request;
        Optional<FriendRequest> existentRequest = repositoryFriendRequests.findOne(getLoggedUser().getId(), friend.getId());
        if (existentRequest.isPresent()) {
            System.out.println("Exista deja cererea!");
        } else {
            request = new FriendRequest(0L, getLoggedUser().getId(), friend.getId(), "pending");
            repositoryFriendRequests.save(request);
        }
    }

    public void deleteFriendRequest(Utilizator friend) {
        Optional<FriendRequest> request = repositoryFriendRequests.findOne(getLoggedUser().getId(), friend.getId());
        if (request.isPresent())
            repositoryFriendRequests.delete(request.get().getId());
        else System.out.println("Cererea nu exista!");
    }

    public List<Utilizator> getAllUsersFromRequests(List<FriendRequest> friendRequests) {
        List<Utilizator> users = new ArrayList<>();
        for (FriendRequest request: friendRequests) {
            if (request.getFromUserId().equals(getLoggedUser().getId()))
                users.add(cautaUtilizator(request.getToUserId()));
            else if (request.getToUserId().equals(getLoggedUser().getId()))
                users.add(cautaUtilizator(request.getFromUserId()));
        }
        return users;
    }

    public List<Utilizator> getAllUtilizatoriByStatus(String status) {
        Iterable<FriendRequest> friendRequests = repositoryFriendRequests.findAll(status);
        Utilizator loggedUser = getLoggedUser();
        List<Utilizator> users = new ArrayList<>();
        for (FriendRequest request: friendRequests) {
            if (request.getFromUserId().equals(loggedUser.getId()) && request.getRequestType().equals(status))
                users.add(cautaUtilizator(request.getToUserId()));
            else if (request.getToUserId().equals(loggedUser.getId()) && request.getRequestType().equals(status))
                users.add(cautaUtilizator(request.getFromUserId()));
        }
        return users;
    }

    public boolean isCorrectPassword(String username, String enteredPassword) {
        return repositoryUtilizatori.isCorrectPassword(username, enteredPassword);
    }

    public void adaugaPrietenie(Long ID1, Long ID2) {
        Prietenie prietenie;
        if (ID1 < ID2)
            prietenie = new Prietenie(ID1, ID2, "approved");
        else
            prietenie = new Prietenie(ID2, ID1, "approved");
        validatorPrietenie.validate(prietenie);
        Utilizator utilizator1 = repositoryUtilizatori.findOne(prietenie.getIdUtilizator1()).orElseThrow(() -> new UtilizatorInexistentException("Utilizatorul nu exista!"));
        Utilizator utilizator2 = repositoryUtilizatori.findOne(prietenie.getIdUtilizator2()).orElseThrow(() -> new UtilizatorInexistentException("Utilizatorul nu exista!"));
        if (getAllPrietenii() != null) {
            getAllPrietenii().forEach(p -> {
                if (p.getIdUtilizator1().equals(prietenie.getIdUtilizator1()) && p.getIdUtilizator2().equals(prietenie.getIdUtilizator2())) {
                    throw new PrietenieExistentaException("Prietenia exista deja!");
                }
            });
            if (repositoryUtilizatori.findOne(prietenie.getIdUtilizator1()).isEmpty() || repositoryUtilizatori.findOne(prietenie.getIdUtilizator2()).isEmpty()) {
                throw new UtilizatorInexistentException("Utilizatorul nu exista!");
            }
            if (prietenie.getIdUtilizator1().equals(prietenie.getIdUtilizator2())) {
                throw new IDuriIdenticeException("ID-urile oferite sunt identice!");
            }
        }
        prietenie.setId(genereazaIdUnicPrietenie());
        repositoryPrietenii.save(prietenie);
        utilizator1.adaugaPrieten(utilizator2);
        utilizator2.adaugaPrieten(utilizator1);
    }

    public void stergePrietenie(Long ID1, Long ID2) {
        Utilizator utilizator1 = repositoryUtilizatori.findOne(ID1).orElseThrow(() -> new UtilizatorInexistentException("Utilizatorul nu exista!"));
        Utilizator utilizator2 = repositoryUtilizatori.findOne(ID2).orElseThrow(() -> new UtilizatorInexistentException("Utilizatorul nu exista!"));

        Long ID = 0L;
        for (Prietenie p : repositoryPrietenii.findAll()) {
            if ((p.getIdUtilizator1().equals(ID1) && p.getIdUtilizator2().equals(ID2)) || (p.getIdUtilizator1().equals(ID2) && p.getIdUtilizator2().equals(ID1)))
                ID = p.getId();
        }
        if (ID == 0L)
            throw new PrietenieInexistentaException("Prietenia nu exista!");
        repositoryPrietenii.delete(ID);
        utilizator1.stergePrieten(utilizator2);
        utilizator2.stergePrieten(utilizator1);
    }

    public Iterable<Prietenie> getAllPrietenii() {
        return repositoryPrietenii.findAll();
    }

    public Map<Utilizator, List<Utilizator>> getAllUtilizatoriCuPrietenii() {
        Map<Utilizator, List<Utilizator>> utilizatoriCuPrieteni = new HashMap<>();
        Iterable<Utilizator> utilizatori = repositoryUtilizatori.findAll();
        for (Utilizator utilizator : utilizatori) {
            List<Utilizator> prieteni = getPrieteniUtilizator(utilizator.getId());
            utilizatoriCuPrieteni.put(utilizator, prieteni);
        }
        return utilizatoriCuPrieteni;
    }

    public List<UtilizatorDatePair> getPrieteniDataPentruUtilizatorInLunaSpecifica(Long utilizatorID, int luna) {
        Utilizator utilizator = cautaUtilizator(utilizatorID);
        return getPrieteniiUtilizator(utilizator.getId())
                .stream()
                .filter(prietenie -> prietenie.getDate().getMonth().getValue() == luna)
                .map(prietenie -> {
                    Utilizator prieten = (prietenie.getIdUtilizator1().equals(utilizatorID)) ?
                            cautaUtilizator(prietenie.getIdUtilizator2()) : cautaUtilizator(prietenie.getIdUtilizator1());
                    return new UtilizatorDatePair(prieten, prietenie.getDate());
                })
                .collect(Collectors.toList());
    }

    private List<Prietenie> getPrieteniiUtilizator(Long utilizatorID) {
        List<Prietenie> prieteniiUtilizator = new ArrayList<>();
        Iterable<Prietenie> prietenii = repositoryPrietenii.findAll();
        for (Prietenie prietenie : prietenii) {
            if (prietenie.getIdUtilizator1().equals(utilizatorID) || prietenie.getIdUtilizator2().equals(utilizatorID))
                prieteniiUtilizator.add(prietenie);
        }
        return prieteniiUtilizator;
    }

    public List<Utilizator> getPrieteniUtilizator(Long utilizatorID) {
        List<Utilizator> prieteni = new ArrayList<>();
        Utilizator utilizator = repositoryUtilizatori.findOne(utilizatorID).orElseThrow(() -> new UtilizatorInexistentException("Utilizatorul nu exista!"));
        Iterable<Prietenie> prietenii = repositoryPrietenii.findAll();
        for (Prietenie prietenie : prietenii) {
            if (prietenie.getIdUtilizator1().equals(utilizatorID)) {
                prieteni.add(repositoryUtilizatori.findOne(prietenie.getIdUtilizator2()).orElse(null));
            } else if (prietenie.getIdUtilizator2().equals(utilizatorID)) {
                prieteni.add(repositoryUtilizatori.findOne(prietenie.getIdUtilizator1()).orElse(null));
            }
        }
        prieteni.removeIf(Objects::isNull);
        return prieteni;
    }

    public List<Utilizator> getCommunityOfLoggedUser() {
        List<List<Utilizator>> allCommunities = getAllComunitati();
        Long loggedUserID = Session.getLoggedUser().getId();

        for (List<Utilizator> community : allCommunities) {
            if (community.stream().anyMatch(user -> user.getId().equals(loggedUserID)))
                return community;
        }
        return Collections.emptyList();
    }

    public List<List<Utilizator>> getAllComunitati() {
        Map<Long, List<Long>> listaAdiacenta = creareListaAdiacenta();
        Set<Long> vizitat = new HashSet<>();
        List<List<Utilizator>> toateComunitatile = new ArrayList<>();

        getAllUtilizatori().forEach(utilizator -> {
            Long utilizatorID = utilizator.getId();
            if (!vizitat.contains(utilizatorID)) {
                List<Utilizator> comunitateCurenta = new ArrayList<>();
                DFS(listaAdiacenta, utilizatorID, vizitat, comunitateCurenta);
                toateComunitatile.add(comunitateCurenta);
            }
        });
        return toateComunitatile;
    }

    public int getNumarComunitati() {
        return (int) getAllComunitati().stream().count();
    }

    public List<Utilizator> ceaMaiSociabilaComunitate() {
        return getAllComunitati()
                .stream()
                .max(Comparator.comparingInt(List::size))
                .orElseGet(ArrayList::new);
    }

    private void DFS(Map<Long, List<Long>> listaAdiacenta, Long userId, Set<Long> vizitat, List<Utilizator> comunitateCurenta) {
        vizitat.add(userId);
        Utilizator utilizator = repositoryUtilizatori.findOne(userId).orElseThrow(() -> new UtilizatorInexistentException("Utilizatorul nu exista!"));
        comunitateCurenta.add(utilizator);

        List<Long> vecini = listaAdiacenta.get(userId);
        if (vecini != null) {
            vecini.forEach(vecin -> {
                if (!vizitat.contains(vecin)) {
                    DFS(listaAdiacenta, vecin, vizitat, comunitateCurenta);
                }
            });
        }
    }

    private Map<Long, List<Long>> creareListaAdiacenta() {
        Map<Long, List<Long>> listaAdiacenta = new HashMap<>();
        getAllPrietenii().forEach(prietenie -> {
            Long user1ID = prietenie.getIdUtilizator1();
            Long user2ID = prietenie.getIdUtilizator2();

            listaAdiacenta.computeIfAbsent(user1ID, k -> new ArrayList<>()).add(user2ID);
            listaAdiacenta.computeIfAbsent(user2ID, k -> new ArrayList<>()).add(user1ID);
        });
        return listaAdiacenta;
    }


    public Utilizator login(String username, String password) {
        Utilizator user = cautaUtilizatorDupaUsername(username);

        if (user != null && user.getPassword().equals(password)) {
            return user;
        } else {
            return null;
        }
    }

    public void logout() {
        Session.clearSession();
    }

    public Utilizator getLoggedUser() {
        return Session.getLoggedUser();
    }


    /* PAGING USERS*/
    public Page<Utilizator> getNextPageUsers() {
        this.pageImplementationUsers = new PageImplementation<>(currentPageUsers.nextPageable(), Stream.empty());
        this.currentPageUsers = repositoryUtilizatori.findAll(pageImplementationUsers.getPageable());
        return repositoryUtilizatori.findAll(currentPageUsers.getPageable());
    }

    public Page<Utilizator> getCurrentPageUsers() {
        return currentPageUsers;
    }

    public Page<Utilizator> getPreviousPageUsers() {
        this.pageImplementationUsers = new PageImplementation<>(currentPageUsers.previousPageable(), Stream.empty());
        this.currentPageUsers = repositoryUtilizatori.findAll(pageImplementationUsers.getPageable());
        return repositoryUtilizatori.findAll(currentPageUsers.getPageable());
    }

    /* PAGING FRIENDS */
    public Page<Utilizator> getNextPageFriends() {
        this.pageImplementationFriends = new PageImplementation<>(currentPageFriendships.nextPageable(), Stream.empty());
        this.currentPageFriendships = repositoryPrietenii.findAll(pageImplementationFriends.getPageable(), getLoggedUser().getId());
        return repositoryPrietenii.findAll(currentPageFriendships.getPageable(), getLoggedUser().getId());
    }

    public Page<Utilizator> getCurrentPageFriends() {
        this.currentPageFriendships = repositoryPrietenii.findAll(pageImplementationFriends.getPageable(), getLoggedUser().getId());
        return currentPageFriendships;
    }

    public Page<Utilizator> getPreviousPageFriends() {
        this.pageImplementationFriends = new PageImplementation<>(currentPageFriendships.previousPageable(), Stream.empty());
        this.currentPageFriendships = repositoryPrietenii.findAll(pageImplementationFriends.getPageable(), getLoggedUser().getId());
        return repositoryPrietenii.findAll(currentPageFriendships.getPageable(), getLoggedUser().getId());
    }

    /* MESSAGES */
    public Message sendMessage(Utilizator from, List<Utilizator> to, String message) {
        Message newMessage = new Message(from, to, message, LocalDateTime.now());
        validatorMessage.validate(newMessage);
        newMessage.setId(genereazaIdUnicMessage());
        repositoryMessages.save(newMessage);
        return newMessage;
    }

    public ReplyMessage replyMessage(Utilizator from, List<Utilizator> to, String message, Message repliedMessage) {
        ReplyMessage reply = new ReplyMessage(from, to, message, LocalDateTime.now(), repliedMessage);
        validatorMessage.validate(reply);
        reply.setId(genereazaIdUnicMessage());
        repositoryMessages.save(reply);
        return reply;
    }

    public Message deleteMessage(Message message) {
        return repositoryMessages.delete(message.getId()).orElseThrow(() -> new MessageInexistentException("Mesajul nu exista!"));
    }

    public Message searchForMessage(Long messageId) {
        return repositoryMessages.findOne(messageId).orElseThrow(() -> new MessageInexistentException("Mesajul nu exista!"));
    }

    public List<Message> getMessagesBetweenUsers(Utilizator user1, Utilizator user2) {
        List<Message> conversation = new ArrayList<>();
        for (Message message: repositoryMessages.findAll()) {
            if ((message.getFrom().equals(user1) && message.getTo().contains(user2))
                    || (message.getFrom().equals(user2) && message.getTo().contains(user1))) {
                conversation.add(message);
            }
        }
        conversation.sort(Comparator.comparing(Message::getDate));
        return conversation;
    }

    public Page<Message> getMessagesBetweenUsersPaginat(Pageable pageable, Long senderID, Long receiverID) {
        this.pageImplementationMessages = new PageImplementation<>(pageable, Stream.empty());
        this.currentPageMessages = repositoryMessages.findAll(pageImplementationMessages.getPageable(), senderID, receiverID);
        return repositoryMessages.findAll(pageable, senderID, receiverID);
    }

    /* PAGING MESSAGES */
    public Page<Message> getNextPageMessages(Long receiverID) {
        this.pageImplementationMessages = new PageImplementation<>(currentPageMessages.nextPageable(), Stream.empty());
        this.currentPageMessages = repositoryMessages.findAll(pageImplementationMessages.getPageable(), getLoggedUser().getId(), receiverID);
        return repositoryMessages.findAll(currentPageMessages.getPageable(), getLoggedUser().getId(), receiverID);
    }

    public Page<Message> getCurrentPageMessages(Long receiverID) {
        this.currentPageMessages = repositoryMessages.findAll(pageImplementationMessages.getPageable(), getLoggedUser().getId(), receiverID);
        return currentPageMessages;
    }

    public Page<Message> getPreviousPageMessages(Long receiverID) {
        this.pageImplementationMessages = new PageImplementation<>(currentPageMessages.previousPageable(), Stream.empty());
        this.currentPageMessages = repositoryMessages.findAll(pageImplementationMessages.getPageable(), getLoggedUser().getId(), receiverID);
        return repositoryMessages.findAll(currentPageMessages.getPageable(), getLoggedUser().getId(), receiverID);
    }

    /* PAGING FRIEND REQUESTS */
    public Page<FriendRequest> getNextPageFriendRequests() {
        this.pageImplementationPendingRequests = new PageImplementation<>(currentPageFriendRequests.nextPageable(), Stream.empty());
        this.currentPageFriendRequests = repositoryFriendRequests.findAll(pageImplementationPendingRequests.getPageable(), getLoggedUser().getId());
        return repositoryFriendRequests.findAll(currentPageFriendRequests.getPageable(), getLoggedUser().getId());
    }

    public Page<FriendRequest> getCurrentPageFriendRequests() {
        this.currentPageFriendRequests = repositoryFriendRequests.findAll(pageImplementationPendingRequests.getPageable(), getLoggedUser().getId());
        return currentPageFriendRequests;
    }

    public Page<FriendRequest> getPreviousPageFriendRequests() {
        this.pageImplementationPendingRequests = new PageImplementation<>(currentPageFriendRequests.previousPageable(), Stream.empty());
        this.currentPageFriendRequests = repositoryFriendRequests.findAll(pageImplementationPendingRequests.getPageable(), getLoggedUser().getId());
        return repositoryFriendRequests.findAll(currentPageFriendRequests.getPageable(), getLoggedUser().getId());
    }
}