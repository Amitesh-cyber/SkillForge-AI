package com.studyrec.repository;

import com.studyrec.model.SavedResource;
import com.studyrec.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedResourceRepository extends JpaRepository<SavedResource, Long> {
    List<SavedResource> findByUser(User user);
}
