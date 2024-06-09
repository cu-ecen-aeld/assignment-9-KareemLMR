/**
 * @file aesdchar.c
 * @brief Functions and data related to the AESD char driver implementation
 *
 * Based on the implementation of the "scull" device driver, found in
 * Linux Device Drivers example code.
 *
 * @author Dan Walkes
 * @date 2019-10-22
 * @copyright Copyright (c) 2019
 *
 */

#include <linux/module.h>
#include <linux/init.h>
#include <linux/printk.h>
#include <linux/types.h>
#include <linux/cdev.h>
#include <linux/fs.h> // file_operations
#include "aesdchar.h"
int aesd_major =   0; // use dynamic major
int aesd_minor =   0;

MODULE_AUTHOR("Kareem Ibrahim"); /** TODO: fill in your name **/
MODULE_LICENSE("Dual BSD/GPL");

struct aesd_dev aesd_device;

int aesd_open(struct inode *inode, struct file *filp)
{
    PDEBUG("open");
    /**
     * TODO: handle open
     */
    struct aesd_dev *dev;
    dev = container_of(inode->i_cdev, struct aesd_dev, cdev);
    //aesd_circular_buffer_init(&dev->buff);
    filp->private_data = dev;

    return 0;
}

int aesd_release(struct inode *inode, struct file *filp)
{
    PDEBUG("release");
    /**
     * TODO: handle release
     */
    return 0;
}

ssize_t aesd_read(struct file *filp, char __user *buf, size_t count,
                loff_t *f_pos)
{
    ssize_t retval = 0;
    PDEBUG("read %zu bytes with offset %lld",count,*f_pos);
    /**
     * TODO: handle read
     */
    struct aesd_dev *dev = filp->private_data;
    size_t offset_rtn = 0;
    size_t prevSize = 0;
    size_t totalSize = 0;
    char* retBuff = kmalloc(count * sizeof(char), GFP_KERNEL);
    int level = 0;
    for (level = 0 ; level < 10 ; level++)
    {
        PDEBUG("string at index = %d is %s ", level, (dev->buff).entry[level].buffptr);
        if (mutex_lock_interruptible(&dev->rw_lock))
        {
            return -ERESTARTSYS;
        }
        struct aesd_buffer_entry *rtnentry = aesd_circular_buffer_find_entry_offset_for_fpos(&dev->buff, totalSize, &offset_rtn);
        mutex_unlock(&dev->rw_lock);

        if (rtnentry == NULL)
        {
            break;
        }
        prevSize = rtnentry->size;
        totalSize += prevSize;
        PDEBUG("prevSize = %d, totalSize = %d, string = %s", prevSize, totalSize, rtnentry->buffptr);
        if (level)
        {
            strcat(retBuff, rtnentry->buffptr);
        }
        else
        {
            strcpy(retBuff, rtnentry->buffptr);
        }
        
        PDEBUG("concatenated string became: %s", retBuff);
        //kfree(rtnentry);
    }

    if (copy_to_user(buf, retBuff, strlen(retBuff)))
    {
		retval = -EFAULT;
	}
    else
    {
        retval = strlen(retBuff);
        *f_pos -= count;
    }
    kfree(retBuff);
    return retval;
}

ssize_t aesd_write(struct file *filp, const char __user *buf, size_t count,
                loff_t *f_pos)
{
    ssize_t retval = -ENOMEM;
    PDEBUG("write %zu bytes with offset %lld",count,*f_pos);
    /**
     * TODO: handle write
     */
    struct aesd_dev *dev = filp->private_data;
    char* newBuff = kmalloc((count + 1) * sizeof(char), GFP_KERNEL);
    if (copy_from_user(newBuff, buf, count))
    {
        PDEBUG("Couldn't copy buf from user space");
        retval = -EFAULT;
    }
    else
    {
        newBuff[count] = '\0';
        if (mutex_lock_interruptible(&dev->cursor_lock))
        {
            return -ERESTARTSYS;
        }
		
        if (dev->entry == NULL)
        {
            dev->entry = kmalloc(sizeof(struct aesd_buffer_entry), GFP_KERNEL);
            dev->entry->buffptr = kmalloc((count + 1) * sizeof(char), GFP_KERNEL);
            dev->entry->buffptr[0] = '\0';
        }
        strcat(dev->entry->buffptr, newBuff);
        if (newBuff[count - 1] == '\n')
        {
            dev->entry->size = strlen(dev->entry->buffptr);
            if (mutex_lock_interruptible(&dev->rw_lock))
            {
                mutex_unlock(&dev->rw_lock);
                return -ERESTARTSYS;
            }
            aesd_circular_buffer_add_entry(&dev->buff, dev->entry);
            mutex_unlock(&dev->rw_lock);
            //kfree(dev->entry->buffptr);
            //kfree(dev->entry);
            dev->entry = NULL;
        }
        mutex_unlock(&dev->cursor_lock);
        *f_pos += count;
        retval = count;
    }
    kfree(newBuff);
    return retval;
}
struct file_operations aesd_fops = {
    .owner =    THIS_MODULE,
    .read =     aesd_read,
    .write =    aesd_write,
    .open =     aesd_open,
    .release =  aesd_release,
};

static int aesd_setup_cdev(struct aesd_dev *dev)
{
    int err, devno = MKDEV(aesd_major, aesd_minor);

    cdev_init(&dev->cdev, &aesd_fops);
    dev->cdev.owner = THIS_MODULE;
    dev->cdev.ops = &aesd_fops;
    err = cdev_add (&dev->cdev, devno, 1);
    if (err) {
        printk(KERN_ERR "Error %d adding aesd cdev", err);
    }
    return err;
}



int aesd_init_module(void)
{
    dev_t dev = 0;
    int result;
    result = alloc_chrdev_region(&dev, aesd_minor, 1,
            "aesdchar");
    aesd_major = MAJOR(dev);
    if (result < 0) {
        printk(KERN_WARNING "Can't get major %d\n", aesd_major);
        return result;
    }
    memset(&aesd_device,0,sizeof(struct aesd_dev));

    /**
     * TODO: initialize the AESD specific portion of the device
     */

    result = aesd_setup_cdev(&aesd_device);

    if( result ) {
        unregister_chrdev_region(dev, 1);
    }
    return result;

}

void aesd_cleanup_module(void)
{
    dev_t devno = MKDEV(aesd_major, aesd_minor);

    cdev_del(&aesd_device.cdev);

    /**
     * TODO: cleanup AESD specific poritions here as necessary
     */

    unregister_chrdev_region(devno, 1);
}



module_init(aesd_init_module);
module_exit(aesd_cleanup_module);
